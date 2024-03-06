import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Main {
    static String CONTENT = "{\"description\": { \"participantInn\": \"string\" }, \"doc_id\": \"string\", \"doc_status\": \"string\", \"doc_type\": \"LP_INTRODUCE_GOODS\", \"importRequest\": true, \"owner_inn\": \"string\", \"participant_inn\": \"string\", \"producer_inn\": \"string\", \"production_date\": \"2020-01-23\", \"production_type\": \"string\", \"products\": [ { \"certificate_document\": \"string\", \"certificate_document_date\": \"2020-01-23\", \"certificate_document_number\": \"string\", \"owner_inn\": \"string\", \"producer_inn\": \"string\", \"production_date\": \"2020-01-23\", \"tnved_code\": \"string\", \"uit_code\": \"string\", \"uitu_code\": \"string\" } ], \"reg_date\": \"2020-01-23\", \"reg_number\": \"string\"}";

    public static void main(String[] args) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        CrptApi crptApi = new CrptApi(TimeUnit.MINUTES, 10, objectMapper, HttpClient.newHttpClient());
        CrptApi.Document document = objectMapper.readValue(CONTENT, CrptApi.Document.class);
        String signature = "Selsup.inc";
        crptApi.createDocument(document, signature);
    }
}

class CrptApi {
    private final TimeUnit timeUnit;
    private final int requestLimit;
    private final ObjectMapper objectMapper;
    private final AtomicInteger requestCount;
    private final Object lock = new Object();
    private final HttpClient httpClient;

    public CrptApi(TimeUnit timeUnit, int requestLimit, ObjectMapper objectMapper, HttpClient httpClient) {
        this.timeUnit = timeUnit;
        this.requestLimit = requestLimit;
        this.objectMapper = objectMapper;
        this.requestCount = new AtomicInteger(0);
        this.httpClient = httpClient;
    }

    public void createDocument(Document document, String signature) {
        synchronized (lock) {
            if (requestCount.get() >= requestLimit) {
                System.out.println("Request limit reached. Waiting for next interval.");
                try {
                    timeUnit.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("Creating document...");

            String jsonDocument;
            try {
                jsonDocument = objectMapper.writeValueAsString(document);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
                return;
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://ismp.crpt.ru/api/v3/lk/documents/create"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonDocument))
                    .build();

            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    System.out.println("Document created successfully.");
                } else {
                    System.out.println("Failed to create document. Status code: " + response.statusCode());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            requestCount.incrementAndGet();
        }
    }

    static class Document {
        @JsonProperty("description")
        private Description description;
        @JsonProperty("doc_id")
        private String docId;
        @JsonProperty("doc_status")
        private String docStatus;
        @JsonProperty("doc_type")
        private String docType;
        @JsonProperty("importRequest")
        private boolean importRequest;
        @JsonProperty("owner_inn")
        private String ownerInn;
        @JsonProperty("participant_inn")
        private String participantInn;
        @JsonProperty("producer_inn")
        private String producerInn;
        @JsonProperty("production_date")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        private String productionDate;
        @JsonProperty("production_type")
        private String productionType;
        @JsonProperty("products")
        private List<Product> products;
        @JsonProperty("reg_date")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        private String regDate;
        @JsonProperty("reg_number")
        private String regNumber;
    }

    static class Description {
        @JsonProperty("participantInn")
        private String participantInn;
    }

    static class Product {
        @JsonProperty("certificate_document")
        private String certificateDocument;
        @JsonProperty("certificate_document_date")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        private String certificateDocumentDate;
        @JsonProperty("certificate_document_number")
        private String certificateDocumentNumber;
        @JsonProperty("owner_inn")
        private String ownerInn;
        @JsonProperty("producer_inn")
        private String producerInn;
        @JsonProperty("production_date")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        private String productionDate;
        @JsonProperty("tnved_code")
        private String tnvedCode;
        @JsonProperty("uit_code")
        private String uitCode;
        @JsonProperty("uitu_code")
        private String uituCode;
    }
}