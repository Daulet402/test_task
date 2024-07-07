import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.ObjectMapper;

public class CrptApi {
    private final HttpClient client;
    private final ObjectMapper objectMapper;
    private final Semaphore semaphore;
    private final long intervalMillis;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.client = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
        this.intervalMillis = timeUnit.toMillis(1);
        this.semaphore = new Semaphore(requestLimit);

        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(intervalMillis);
                    semaphore.release(requestLimit - semaphore.availablePermits());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }).start();
    }

    public void createDocument(Document document, String signature) throws IOException, InterruptedException {
        semaphore.acquire();
        String json = objectMapper.writeValueAsString(document);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://ismp.crpt.ru/api/v3/lk/documents/create"))
                .header("Content-Type", "application/json")
                .header("Signature", signature)
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    public static class Document {
        public Description description;
        public String doc_id;
        public String doc_status;
        public String doc_type;
        public boolean importRequest;
        public String owner_inn;
        public String participant_inn;
        public String producer_inn;
        public String production_date;
        public String production_type;
        public Product[] products;
        public String reg_date;
        public String reg_number;

        public static class Description {
            public String participantInn;
        }

        public static class Product {
            public String certificate_document;
            public String certificate_document_date;
            public String certificate_document_number;
            public String owner_inn;
            public String producer_inn;
            public String production_date;
            public String tnved_code;
            public String uit_code;
            public String uitu_code;
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        CrptApi api = new CrptApi(TimeUnit.MINUTES, 10);

        Document document = new Document();
        document.doc_id = "doc123";
        document.doc_status = "NEW";
        document.doc_type = "LP_INTRODUCE_GOODS";
        document.importRequest = true;
        document.owner_inn = "1234567890";
        document.participant_inn = "1234567890";
        document.producer_inn = "1234567890";
        document.production_date = "2020-01-23";
        document.production_type = "type";
        document.reg_date = "2020-01-23";
        document.reg_number = "reg123";
        document.description = new Document.Description();
        document.description.participantInn = "1234567890";

        Document.Product product = new Document.Product();
        product.certificate_document = "cert";
        product.certificate_document_date = "2020-01-23";
        product.certificate_document_number = "123";
        product.owner_inn = "1234567890";
        product.producer_inn = "1234567890";
        product.production_date = "2020-01-23";
        product.tnved_code = "code";
        product.uit_code = "uit";
        product.uitu_code = "uitu";
        document.products = new Document.Product[]{product};

        api.createDocument(document, "signature_value_goes_here");
    }
}
