package com.walmart.emailService;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder;
import com.amazonaws.services.simpleemail.model.BulkEmailDestination;
import com.amazonaws.services.simpleemail.model.Destination;
import com.amazonaws.services.simpleemail.model.SendBulkTemplatedEmailRequest;
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

    private AmazonSimpleEmailService emailClient;
    private static final String SSM_EMAIL_KEY = "EMAIL_KEY";

    private static String dataFormat = "{ \"user\":\"%s\" }";

    public EmailHandler() {
        ssmClient = AWSSimpleSystemsManagementClientBuilder.defaultClient();
        emailClient = AmazonSimpleEmailServiceClientBuilder.defaultClient();
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
            System.out.println(record.getEventSourceARN());
            String eventName = record.getEventName();
            System.out.println(record.getEventName());
            switch (eventName) {
                case "INSERT":
                    System.out.println("inside the insert case");
                    insertedEmails.add(email);
                    break;
                case "REMOVE":
                    deletedEmails.add(email);
                    break;
                default:
                    System.out.println("not a valid event to send emails");
                    break;
            }
            System.out.println(insertedEmails.size());
            System.out.println(record.getDynamodb().toString());
        }

        List<BulkEmailDestination> bulkEmailDestinationList = new ArrayList<>();

        for(String insertEmail: insertedEmails) {
            Destination destination = new Destination();
            destination.setToAddresses(List.of(insertEmail));
            BulkEmailDestination bulkEmailDestination = new BulkEmailDestination();
            bulkEmailDestination.setDestination(destination);
            bulkEmailDestination.setReplacementTemplateData(String.format(dataFormat, insertEmail));
            bulkEmailDestinationList.add(bulkEmailDestination);
        }

        SendBulkTemplatedEmailRequest bulkTemplatedEmailRequest = new SendBulkTemplatedEmailRequest();
        bulkTemplatedEmailRequest.withDestinations(bulkEmailDestinationList);
        bulkTemplatedEmailRequest.withTemplate("AddressInsertTemplate");
        bulkTemplatedEmailRequest.withSource(email);
        bulkTemplatedEmailRequest.withDefaultTemplateData(String.format(dataFormat, "User"));

        try {
            emailClient.sendBulkTemplatedEmail(bulkTemplatedEmailRequest);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        return null;
    }
}
