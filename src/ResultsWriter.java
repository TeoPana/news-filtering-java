import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class ResultsWriter {
    private final Statistics stats;
    private final Set<String> validLanguages;
    private final Set<String> validCategories;

    public ResultsWriter(Statistics stats, Set<String> validLanguages, Set<String> validCategories) {
        this.stats = stats;
        this.validLanguages = validLanguages;
        this.validCategories = validCategories;
    }

    public void writeAllOutputs() throws IOException {
        writeAllArticlesFile();
        writeCategoryFiles();
        writeLanguageFiles();
        writeKeywordsFile();
        writeReportsFile();
    }

    private void writeAllArticlesFile() throws IOException {
        List<Article> articles = stats.getUniqueArticles();
        
        articles.sort((a1, a2) -> {
            int cmp = a2.getPublished().compareTo(a1.getPublished());
            if (cmp != 0) return cmp;
            return a1.getUuid().compareTo(a2.getUuid());
        });

        try (PrintWriter writer = new PrintWriter(new FileWriter("all_articles.txt"))) {
            for (Article article : articles) {
                writer.println(article.getUuid() + " " + article.getPublished());
            }
        }
    }

    private void writeCategoryFiles() throws IOException {
        Map<String, Set<String>> categoryArticles = new HashMap<>();
        
        for (Article article : stats.getUniqueArticles()) {
            if (article.getCategories() != null) {
                for (String category : article.getCategories()) {
                    if (validCategories.contains(category)) {
                        String normalizedCategory = normalizeCategory(category);
                        // Use Set to avoid duplicate UUIDs
                        categoryArticles.computeIfAbsent(normalizedCategory, k -> new HashSet<>())
                            .add(article.getUuid());
                    }
                }
            }
        }

        for (Map.Entry<String, Set<String>> entry : categoryArticles.entrySet()) {
            List<String> uuids = new ArrayList<>(entry.getValue());
            Collections.sort(uuids);
            
            try (PrintWriter writer = new PrintWriter(new FileWriter(entry.getKey() + ".txt"))) {
                for (String uuid : uuids) {
                    writer.println(uuid);
                }
            }
        }
    }

    private void writeLanguageFiles() throws IOException {
        Map<String, Set<String>> languageArticles = new HashMap<>();
        
        for (Article article : stats.getUniqueArticles()) {
            String lang = article.getLanguage();
            if (validLanguages.contains(lang)) {
                // Use Set to avoid duplicate UUIDs
                languageArticles.computeIfAbsent(lang, k -> new HashSet<>())
                    .add(article.getUuid());
            }
        }

        for (Map.Entry<String, Set<String>> entry : languageArticles.entrySet()) {
            List<String> uuids = new ArrayList<>(entry.getValue());
            Collections.sort(uuids);
            
            try (PrintWriter writer = new PrintWriter(new FileWriter(entry.getKey() + ".txt"))) {
                for (String uuid : uuids) {
                    writer.println(uuid);
                }
            }
        }
    }

    private void writeKeywordsFile() throws IOException {
        Map<String, Set<String>> keywordArticles = stats.getKeywordArticles();
        
        List<Map.Entry<String, Integer>> sortedKeywords = keywordArticles.entrySet().stream()
            .map(e -> new AbstractMap.SimpleEntry<>(e.getKey(), e.getValue().size()))
            .sorted((e1, e2) -> {
                int cmp = e2.getValue().compareTo(e1.getValue());
                if (cmp != 0) return cmp;
                return e1.getKey().compareTo(e2.getKey());
            })
            .collect(Collectors.toList());

        try (PrintWriter writer = new PrintWriter(new FileWriter("keywords_count.txt"))) {
            for (Map.Entry<String, Integer> entry : sortedKeywords) {
                writer.println(entry.getKey() + " " + entry.getValue());
            }
        }
    }

    private void writeReportsFile() throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter("reports.txt"))) {
            writer.println("duplicates_found - " + stats.getDuplicatesFound());
            writer.println("unique_articles - " + stats.getUniqueArticlesCount());
            
            Map.Entry<String, Integer> bestAuthor = stats.getBestAuthor();
            if (bestAuthor != null) {
                writer.println("best_author - " + bestAuthor.getKey() + " " + bestAuthor.getValue());
            }
            
            Map.Entry<String, Integer> topLang = stats.getTopLanguage();
            if (topLang != null) {
                writer.println("top_language - " + topLang.getKey() + " " + topLang.getValue());
            }
            
            Map.Entry<String, Integer> topCat = stats.getTopCategory();
            if (topCat != null) {
                String normalizedCat = normalizeCategory(topCat.getKey());
                writer.println("top_category - " + normalizedCat + " " + topCat.getValue());
            }
            
            Article recent = stats.getMostRecentArticle();
            if (recent != null) {
                writer.println("most_recent_article - " + recent.getPublished() + " " + recent.getUrl());
            }
            
            Map<String, Set<String>> keywords = stats.getKeywordArticles();
            if (!keywords.isEmpty()) {
                Map.Entry<String, Integer> topKeyword = keywords.entrySet().stream()
                    .map(e -> new AbstractMap.SimpleEntry<>(e.getKey(), e.getValue().size()))
                    .max(Comparator.comparing((Map.Entry<String, Integer> e) -> e.getValue())
                        .thenComparing(Comparator.comparing((Map.Entry<String, Integer> e) -> e.getKey()).reversed()))
                    .orElse(null);
                
                if (topKeyword != null) {
                    writer.println("top_keyword_en - " + topKeyword.getKey() + " " + topKeyword.getValue());
                }
            }
        }
    }

    private String normalizeCategory(String category) {
        return category.replace(",", "").replaceAll("\\s+", "_");
    }
}