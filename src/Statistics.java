import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Statistics {
    // Store ALL articles first
    private final List<Article> allArticles = Collections.synchronizedList(new ArrayList<>());
    
    // After filtering
    private List<Article> uniqueArticles = new ArrayList<>();
    private int duplicatesFound = 0;
    
    // Statistics
    private final Map<String, Integer> authorCounts = new ConcurrentHashMap<>();
    private final Map<String, Integer> languageCounts = new ConcurrentHashMap<>();
    private final Map<String, Integer> categoryCounts = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> keywordArticles = new ConcurrentHashMap<>();
    private Article mostRecentArticle = null;

    // Just add articles, don't filter yet
    public synchronized void addArticle(Article article) {
        allArticles.add(article);
    }
    
    // Batch add to reduce synchronization overhead
    public synchronized void addArticles(List<Article> articles) {
        allArticles.addAll(articles);
    }

    // Filter duplicates after all articles are loaded
    public void filterDuplicates() {
        // Count UUID and title occurrences
        Map<String, Integer> uuidCount = new HashMap<>();
        Map<String, Integer> titleCount = new HashMap<>();
        
        for (Article article : allArticles) {
            uuidCount.merge(article.getUuid(), 1, Integer::sum);
            titleCount.merge(article.getTitle(), 1, Integer::sum);
        }
        
        // Keep only articles where both UUID and title appear exactly once
        for (Article article : allArticles) {
            if (uuidCount.get(article.getUuid()) > 1 || titleCount.get(article.getTitle()) > 1) {
                duplicatesFound++;
            } else {
                uniqueArticles.add(article);
                
                // Update stats
                authorCounts.merge(article.getAuthor(), 1, Integer::sum);
                languageCounts.merge(article.getLanguage(), 1, Integer::sum);
                
                // Count each category only once per article (use Set to deduplicate)
                if (article.getCategories() != null) {
                    Set<String> uniqueCategories = new HashSet<>(article.getCategories());
                    for (String category : uniqueCategories) {
                        categoryCounts.merge(category, 1, Integer::sum);
                    }
                }
                
                if (mostRecentArticle == null || 
                    article.getPublished().compareTo(mostRecentArticle.getPublished()) > 0 ||
                    (article.getPublished().equals(mostRecentArticle.getPublished()) && article.getUuid().compareTo(mostRecentArticle.getUuid()) < 0)) {
                    mostRecentArticle = article;
                }
            }
        }
    }

    public void addKeyword(String keyword, String articleUuid) {
        keywordArticles.computeIfAbsent(keyword, k -> ConcurrentHashMap.newKeySet()).add(articleUuid);
    }

    public List<Article> getUniqueArticles() {
        return uniqueArticles;
    }

    public int getDuplicatesFound() {
        return duplicatesFound;
    }

    public int getUniqueArticlesCount() {
        return uniqueArticles.size();
    }

    public Map.Entry<String, Integer> getBestAuthor() {
        return authorCounts.entrySet().stream()
            .max(Comparator.comparing((Map.Entry<String, Integer> e) -> e.getValue())
                .thenComparing(Comparator.comparing((Map.Entry<String, Integer> e) -> e.getKey()).reversed()))
            .orElse(null);
    }

    public Map.Entry<String, Integer> getTopLanguage() {
        return languageCounts.entrySet().stream()
            .max(Comparator.comparing((Map.Entry<String, Integer> e) -> e.getValue())
                .thenComparing(Comparator.comparing((Map.Entry<String, Integer> e) -> e.getKey()).reversed()))
            .orElse(null);
    }

    public Map.Entry<String, Integer> getTopCategory() {
        return categoryCounts.entrySet().stream()
            .max(Comparator.comparing((Map.Entry<String, Integer> e) -> e.getValue())
                .thenComparing(Comparator.comparing((Map.Entry<String, Integer> e) -> e.getKey()).reversed()))
            .orElse(null);
    }

    public Article getMostRecentArticle() {
        return mostRecentArticle;
    }

    public Map<String, Set<String>> getKeywordArticles() {
        return keywordArticles;
    }
}