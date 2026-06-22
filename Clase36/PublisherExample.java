import com.google.cloud.pubsub.v1.Publisher;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.ProjectTopicName;
import com.google.pubsub.v1.PubsubMessage;
import java.util.concurrent.TimeUnit;

public class PublisherExample {
    public static void main(String... args) throws Exception {
        String projectId = "servicio-pub-sub";
        String topicId = "tema";

        ProjectTopicName topicName = ProjectTopicName.of(projectId, topicId);
        Publisher publisher = Publisher.newBuilder(topicName).build();

        try {
            String mensaje = "Publicando con Java. Mensaje 1.";
            ByteString data = ByteString.copyFromUtf8(mensaje);
            PubsubMessage pubsubMessage = PubsubMessage.newBuilder().setData(data).build();

            publisher.publish(pubsubMessage);
            System.out.println("Mensaje publicado: " + mensaje);

        } finally {
            publisher.shutdown();
            publisher.awaitTermination(1, TimeUnit.MINUTES);
        }
    }
}
