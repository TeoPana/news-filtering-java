import java.io.*;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        if (args.length != 3) {
            System.err.println("Usage: java Main <num_threads> <articles_file> <inputs_file>");
            System.exit(1);
        }

        int numThreads = Integer.parseInt(args[0]);
        String articlesFile = args[1];
        String inputsFile = args[2];

        try {
            // Read input files
            List<String> articleFiles = readArticleFiles(articlesFile);
            String[] auxiliaryFiles = readAuxiliaryFiles(inputsFile);
            
            Set<String> languages = readLanguages(auxiliaryFiles[0]);
            Set<String> categories = readCategories(auxiliaryFiles[1]);
            Set<String> linkingWords = readLinkingWords(auxiliaryFiles[2]);

            // Initialize shared data structures
            Queue<String> fileQueue = new LinkedList<>(articleFiles);
            Statistics stats = new Statistics();

            // Create and start threads for reading articles
            Thread[] threads = new Thread[numThreads];
            for (int i = 0; i < numThreads; i++) {
                threads[i] = new Worker(fileQueue, stats, linkingWords);
                threads[i].start();
            }

            // Wait for all threads to finish
            for (Thread thread : threads) {
                thread.join();
            }

            // Filter duplicates AFTER all articles are loaded
            stats.filterDuplicates();
            
            // Process keywords in parallel for unique English articles
            List<Article> englishArticles = new ArrayList<>();
            for (Article article : stats.getUniqueArticles()) {
                if ("english".equals(article.getLanguage())) {
                    englishArticles.add(article);
                }
            }
            
            // Create keyword processing queue
            Queue<Article> keywordQueue = new LinkedList<>(englishArticles);
            Thread[] keywordThreads = new Thread[numThreads];
            
            for (int i = 0; i < numThreads; i++) {
                keywordThreads[i] = new Thread(() -> {
                    while (true) {
                        Article article;
                        synchronized (keywordQueue) {
                            if (keywordQueue.isEmpty()) break;
                            article = keywordQueue.poll();
                        }
                        if (article != null) {
                            processKeywords(article, stats, linkingWords);
                        }
                    }
                });
                keywordThreads[i].start();
            }
            
            for (Thread t : keywordThreads) {
                t.join();
            }

            // Write outputs
            ResultsWriter writer = new ResultsWriter(stats, languages, categories);
            writer.writeAllOutputs();

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void processKeywords(Article article, Statistics stats, Set<String> linkingWords) {
        if (article.getText() == null) return;
        
        String text = article.getText().toLowerCase();
        String[] words = text.split("\\s+");
        
        Set<String> uniqueWords = new HashSet<>();
        for (String word : words) {
            // Remove non-letter characters
            String cleaned = word.replaceAll("[^a-z]", "");
            
            if (!cleaned.isEmpty() && !linkingWords.contains(cleaned)) {
                uniqueWords.add(cleaned);
            }
        }
        
        // Add each unique word with this article's UUID
        for (String keyword : uniqueWords) {
            stats.addKeyword(keyword, article.getUuid());
        }
    }

    private static List<String> readArticleFiles(String filename) throws IOException {
        List<String> files = new ArrayList<>();
        File inputFile = new File(filename);
        String basePath = inputFile.getParent();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            int n = Integer.parseInt(reader.readLine().trim());
            for (int i = 0; i < n; i++) {
                String relativePath = reader.readLine().trim();
                // Resolve relative to the articles.txt location
                if (basePath != null) {
                    files.add(new File(basePath, relativePath).getPath());
                } else {
                    files.add(relativePath);
                }
            }
        }
        return files;
    }

    private static String[] readAuxiliaryFiles(String filename) throws IOException {
        String[] files = new String[3];
        File inputFile = new File(filename);
        String basePath = inputFile.getParent();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            int n = Integer.parseInt(reader.readLine().trim());
            for (int i = 0; i < n && i < 3; i++) {
                String relativePath = reader.readLine().trim();
                // Resolve relative to the inputs.txt location
                if (basePath != null) {
                    files[i] = new File(basePath, relativePath).getPath();
                } else {
                    files[i] = relativePath;
                }
            }
        }
        return files;
    }

    private static Set<String> readLanguages(String filename) throws IOException {
        Set<String> languages = new HashSet<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            int n = Integer.parseInt(reader.readLine().trim());
            for (int i = 0; i < n; i++) {
                languages.add(reader.readLine().trim());
            }
        }
        return languages;
    }

    private static Set<String> readCategories(String filename) throws IOException {
        Set<String> categories = new HashSet<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            int n = Integer.parseInt(reader.readLine().trim());
            for (int i = 0; i < n; i++) {
                categories.add(reader.readLine().trim());
            }
        }
        return categories;
    }

    private static Set<String> readLinkingWords(String filename) throws IOException {
        Set<String> words = new HashSet<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            int n = Integer.parseInt(reader.readLine().trim());
            for (int i = 0; i < n; i++) {
                words.add(reader.readLine().trim().toLowerCase());
            }
        }
        return words;
    }
}