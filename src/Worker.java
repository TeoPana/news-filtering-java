import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.util.*;

public class Worker extends Thread {
    private final Queue<String> fileQueue;
    private final Statistics stats;
    private final ObjectMapper mapper;

    public Worker(Queue<String> fileQueue, Statistics stats, Set<String> linkingWords) {
        this.fileQueue = fileQueue;
        this.stats = stats;
        this.mapper = new ObjectMapper();
    }

    @Override
    public void run() {
        while (true) {
            String filePath;
            synchronized (fileQueue) {
                if (fileQueue.isEmpty()) {
                    break;
                }
                filePath = fileQueue.poll();
            }

            if (filePath != null) {
                processFile(filePath);
            }
        }
    }

    private void processFile(String filePath) {
        try {
            Article[] articles = mapper.readValue(new File(filePath), Article[].class);
            
            for (Article article : articles) {
                // Add ALL articles first (duplicate detection happens later)
                stats.addArticle(article);
            }
        } catch (IOException e) {
            System.err.println("Error processing file: " + filePath);
            e.printStackTrace();
        }
    }
}