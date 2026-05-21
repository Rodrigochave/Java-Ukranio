package com.mycompany.app;

import com.google.cloud.pubsub.v1.AckReplyConsumer;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.pubsub.v1.ProjectSubscriptionName;
import com.google.pubsub.v1.PubsubMessage;

public class SubscriberExample {
    public static void main(String... args) throws Exception {
        String projectId = "servicio-pub-sub";          // Mismo que el publicador
        String subscriptionId = "new_transaction-sub"; // La suscripción creada
        ProjectSubscriptionName subscriptionName =
                ProjectSubscriptionName.of(projectId, subscriptionId);

        MessageReceiver receiver = (PubsubMessage message, AckReplyConsumer consumer) -> {
            String contenido = message.getData().toStringUtf8();
            System.out.println("Mensaje recibido: " + contenido);
            consumer.ack();
        };

        Subscriber subscriber = Subscriber.newBuilder(subscriptionName, receiver).build();
        subscriber.startAsync().awaitRunning();
        System.out.println("Subscriber activo. Esperando mensajes (Ctrl + C para salir)...");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n Cerrando subscriber...");
            subscriber.stopAsync();
            System.out.println(" Subscriber detenido correctamente.");
        }));

        Thread.currentThread().join();
    }
}