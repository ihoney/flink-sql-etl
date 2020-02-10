package kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;

public class OrderSender {
    private static final Logger logger = LoggerFactory.getLogger(CurrencySender.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Random random = new Random();
    private static final SendCallBack sendCallBack = new SendCallBack();
    private static final String topicName = "flink_orders3";
    private static final List<String> currencies = initCurrencies();
    private static final List<String> itemNames = initItemNames();

    public static synchronized void sendMessage(Properties kafkaProperties, int continueMinutes) throws InterruptedException, JsonProcessingException {
        KafkaProducer<Object, Object> producer = new KafkaProducer<>(kafkaProperties);
        // order stream
        for (int i = 0; i < continueMinutes * 60; i++) {
            long timestart = System.currentTimeMillis();
            for (int j = 0; j < currencies.size(); j++) {
                Map<String, Object> map = new HashMap<>();
                map.put("order_id", System.currentTimeMillis() + "_" + random.nextInt());
                map.put("item", itemNames.get(j % itemNames.size()));
                map.put("currency", currencies.get(j % currencies.size()));
                map.put("amount", j % 100 / 100.0);
                Long time = System.currentTimeMillis();
                DateFormat dateFormat =  new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
                Date date = new Date(time);
                String jsonSchemaDate = dateFormat.format(date);
                map.put("order_time", jsonSchemaDate);
                producer.send(
                        new ProducerRecord<>(
                                topicName,
                                String.valueOf(time),
                                objectMapper.writeValueAsString(map)
                        ), sendCallBack

                );
                Thread.sleep(5);

            }
            long timecast = System.currentTimeMillis() - timestart;
            System.out.println((i + 1) * currencies.size() + " has sended to topic:[" + topicName + "] in " + timecast + "ms");
            if (timecast < 2000) {
                System.out.println("begin sleep...." + System.currentTimeMillis());
                Thread.sleep(2000);
                System.out.println("end sleep...." + System.currentTimeMillis());

            }
        }
    }

    static class SendCallBack implements Callback {

        @Override
        public void onCompletion(RecordMetadata recordMetadata, Exception e) {
            if (e != null) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    private static List<String> initCurrencies() {
        final List<String> currencies = new ArrayList<>();
        currencies.add("US Dollar");
        currencies.add("Euro");
        currencies.add("Yen");
        return currencies;
    }

    private static List<String> initItemNames() {
        final List<String> itermNames = new ArrayList<>();
        itermNames.add("Apple");
        itermNames.add("Orange");
        itermNames.add("Paper");
        itermNames.add("Milk");
        itermNames.add("Yogurt");
        return itermNames;
    }
}