package com.walmart.emailService;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClientBuilder;
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterRequest;
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class EmailHandler implements RequestHandler<DynamodbEvent, Void> {

    private AWSSimpleSystemsManagement ssmClient;
    private static final String SSM_EMAIL_KEY = "EMAIL_KEY";

    public EmailHandler() {
        ssmClient = AWSSimpleSystemsManagementClientBuilder.defaultClient();
    }
    private static final Logger logger = LogManager.getLogger(EmailHandler.class);

    @Override
    public Void handleRequest(DynamodbEvent dynamodbEvent, Context context) {
        System.out.println("inside handle request");

        List<String> insertedEmails = new ArrayList<>();
        List<String> deletedEmails = new ArrayList<>();
        List<String> updatedEmails = new ArrayList<>();

        //Getting email from SSM parameter store
        GetParameterRequest request = new GetParameterRequest();
        request.withName(SSM_EMAIL_KEY);
        GetParameterResult result = ssmClient.getParameter(request);
        String email = result.getParameter().getValue();
        System.out.println(email);

        // In next iteration, we will decrypt the bearer token and
        // get the user details to send the email
        for(DynamodbEvent.DynamodbStreamRecord record: dynamodbEvent.getRecords()) {
            logger.info(record.getEventSourceARN());
            System.out.println(record.getEventName());
            switch(record.getEventName()) {
                case "INSERT":
                    insertedEmails.add(email);
                case "REMOVE":
                    deletedEmails.add(email);
                default:
                    System.out.println("not a valid event to send emails");
                    break;
            }
            System.out.println(record.getDynamodb().toString());
        }

        return null;
    }
}
