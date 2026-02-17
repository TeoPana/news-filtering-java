# Article Processing Pipeline

This project reads multiple JSON article files, removes duplicates, computes statistics, extracts keywords from English articles, and writes several output reports.

## Quick Start

Build:

```bash
cd src
make build
```

Run:

```bash
cd src
make run ARGS="<num_threads> <articles_file> <inputs_file>"
```

`<articles_file>` and `<inputs_file>` are text files that list other input files. The program resolves their paths relative to the directory where those list files live.

## Code Map

- `src/Main.java`: Orchestrates the full pipeline. Parses CLI args, loads configuration files, starts worker threads to read articles, removes duplicates, extracts keywords in parallel, then writes all outputs.
- `src/Worker.java`: Thread class that reads one JSON file at a time and feeds `Article` objects into `Statistics`.
- `src/Statistics.java`: Central in-memory store and aggregator for articles, duplicates, and computed statistics.
- `src/ResultsWriter.java`: Writes all output files (articles list, categories, languages, keyword counts, and a summary report).
- `src/Article.java`: POJO used by Jackson to deserialize JSON article objects.

## Detailed Flow

1. **Input parsing (Main)**
   - Expects 3 arguments: `num_threads`, `articles_file`, `inputs_file`.
   - `articles_file` contains an integer `n`, followed by `n` relative paths to JSON files containing articles.
   - `inputs_file` contains an integer `n`, followed by up to 3 relative paths:
     - languages list
     - categories list
     - linking words list

2. **Load configuration lists**
   - Languages, categories, and linking words are read into `Set`s for fast membership checks.

3. **Read all articles in parallel**
   - A shared `Queue<String>` holds JSON file paths.
   - `num_threads` `Worker` threads consume the queue and parse files with Jackson.
   - Each parsed `Article` is pushed into `Statistics` using a synchronized list (`allArticles`).

4. **Deduplicate and compute base stats**
   - After all threads finish, `Statistics.filterDuplicates()`:
     - Counts duplicates by both UUID and title.
     - Keeps only articles where both UUID and title appear exactly once.
     - Updates author, language, category counts, and the most recent article.

5. **Extract keywords in parallel**
   - Only unique English articles are processed for keywords.
   - A second thread pool consumes `Article` objects and extracts unique words.
   - Words are lowercased, stripped to `a-z`, and filtered by `linking words`.
   - Each keyword is mapped to the set of article UUIDs it appears in.

6. **Write outputs**
   - `ResultsWriter` creates all output files in the current directory.

## Class Explanations

### `Article`
A simple Java bean used for JSON deserialization. It matches the JSON fields present in the input files and ignores any unknown fields (`@JsonIgnoreProperties(ignoreUnknown = true)`).

### `Worker`
- Each worker repeatedly pops a file path from a synchronized queue.
- It deserializes the JSON array into `Article[]` using Jackson's `ObjectMapper`.
- Articles are pushed into `Statistics` without filtering; deduplication happens later.

### `Statistics`
Stores and computes all derived data:

- **Storage**:
  - `allArticles`: synchronized list that receives all articles.
  - `uniqueArticles`: list built after deduplication.

- **Deduplication logic**:
  - An article is considered a duplicate if **its UUID or its title appears more than once**.
  - Only articles with a unique UUID **and** a unique title are kept.

- **Computed stats**:
  - Author count, language count, category count.
  - Most recent article (by `published` date, tie broken by smallest UUID).
  - Keyword map: keyword -> set of UUIDs containing it.

- **Concurrency**:
  - Uses `ConcurrentHashMap` for counters and keyword sets.

### `ResultsWriter`
Writes output files based on `Statistics` and the valid languages/categories lists:

- `all_articles.txt`
  - Unique articles sorted by `published` desc, tie broken by `uuid`.
  - One line: `<uuid> <published>`.

- `<category>.txt`
  - For each valid category found in articles.
  - Category names are normalized: commas removed, spaces collapsed to underscores.
  - Lines are sorted UUIDs.

- `<language>.txt`
  - For each valid language found in articles.
  - Lines are sorted UUIDs.

- `keywords_count.txt`
  - Keywords sorted by frequency desc, tie broken by keyword asc.
  - One line: `<keyword> <count>`.

- `reports.txt`
  - Summary statistics:
    - `duplicates_found`
    - `unique_articles`
    - `best_author` (highest count, tie by lexicographically larger name)
    - `top_language` (highest count, tie by lexicographically larger name)
    - `top_category` (highest count, tie by lexicographically larger name)
    - `most_recent_article` (published date and URL)
    - `top_keyword_en` (highest keyword count in English articles, tie by lexicographically larger keyword)

## Concurrency Notes

- Input reading uses multiple `Worker` threads with a shared queue.
- Keyword processing uses another pool of threads on a shared queue of `Article` objects.
- Updates to shared counters and sets are thread-safe using `ConcurrentHashMap` and synchronized lists/queues.

## File Locations

All outputs are written to the directory where the program is run (typically `src/`).

## Requirements

- Java
- Jackson libraries (`jackson-databind`, `jackson-core`, `jackson-annotations`), downloaded by `make build`.
