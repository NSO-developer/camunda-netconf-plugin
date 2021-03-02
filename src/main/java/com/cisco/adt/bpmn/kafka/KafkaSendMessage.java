package com.cisco.adt.bpmn.kafka;

import com.cisco.adt.data.ReturnCodes;
import com.cisco.adt.data.model.bpmn.TaskResult;
import com.cisco.adt.util.Utils;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/**
 *  Sample plugin for posting a message to a kafka bus
 *  Kafka server address and topic, as well as the message to be sent are received asa input variables
 *  Client ID is statically set to "Karajan"
 *  Will fill a @{@link TaskResult} object back to the workflow process, containing the result code (OK or not), a detail in case of error
 */
public class KafkaSendMessage implements JavaDelegate {

	private Logger logger = LoggerFactory.getLogger(KafkaSendMessage.class);


	public static final String CLIENT_ID = "Karajan";


	@Override
	public void execute(DelegateExecution execution) {

        String kafkaAddress = (String) execution.getVariable("kafkaAddress");
        String topic = (String) execution.getVariable("topic");
        String message = (String) execution.getVariable("message");


        if (!kafkaAddress.contains(":")) {
            kafkaAddress += ":9092";
        }

        TaskResult taskResult = new TaskResult();

		Properties properties = new Properties();
		properties.put("bootstrap.servers", kafkaAddress);
		properties.put("client.id", CLIENT_ID);
		properties.put("key.serializer", "org.apache.kafka.common.serialization.IntegerSerializer");
		properties.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
		KafkaProducer producer = new KafkaProducer<>(properties);
		ProducerRecord<String, String> producerRecord = new ProducerRecord<>(topic, message);
		try {
		    producer.send(producerRecord);
            producer.flush();
        } catch (Exception e) {
            taskResult.setCode(ReturnCodes.ERROR);
            taskResult.setDetail(Utils.getRootException(e).getMessage());
            execution.setVariableLocal("taskResult", taskResult);
            logger.debug(ReturnCodes.ERROR + ", " + Utils.getRootException(e).getMessage());
        } finally {
            producer.close();
        }

        taskResult.setCode(ReturnCodes.OK);
        execution.setVariableLocal("taskResult", taskResult);
        logger.debug(ReturnCodes.OK);

    }
}
