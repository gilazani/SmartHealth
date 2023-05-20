package superapp.logic.Mongo;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import superapp.Boundary.*;
import superapp.Boundary.User.UserId;
import superapp.dal.MiniAppCommandRepository;
import superapp.dal.UserRepository;
import superapp.data.Enum.UserRole;
import superapp.data.mainEntity.MiniAppCommandEntity;
import superapp.data.mainEntity.UserEntity;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import superapp.logic.Exceptions.DepreacatedOpterationException;
import superapp.logic.Exceptions.PermissionDeniedException;
import superapp.logic.Exceptions.UserNotFoundException;
import superapp.logic.service.MiniAppCommandServiceWithAdminPermission;
import superapp.logic.utilitys.GeneralUtility;

import java.util.*;
import java.util.stream.Collectors;
@Service
public class MiniAppCommandServiceRepo implements MiniAppCommandServiceWithAdminPermission {

    private  String springApplicationName;
    private final MiniAppCommandRepository repository;
    private final UserRepository userRepository;//for permission checks
    private final MongoTemplate mongoTemplate;

    private JmsTemplate jmsTemplate;

    private ObjectMapper jackson;



    // this method injects a configuration value of spring
    @Value("${spring.application.name:iAmTheDefaultNameOfTheApplication}")
    public void setSpringApplicationName(String springApplicationName) {
        this.springApplicationName = springApplicationName;
    }

    @PostConstruct
    public void init() {
        if (!mongoTemplate.collectionExists("COMMAND")) {
            mongoTemplate.createCollection("COMMAND");
            this.jackson = new ObjectMapper();
        }
        if (!mongoTemplate.collectionExists("USERS")) {
            mongoTemplate.createCollection("USERS");
        }
    }

    @Autowired
    public void setJmsTemplate(JmsTemplate jmsTemplate) {
        this.jmsTemplate = jmsTemplate;
        this.jmsTemplate.setDeliveryDelay(5000L);
    }


    @Override
    public MiniAppCommandBoundary asyncHandle(MiniAppCommandBoundary miniAppCommandBoundary) {
        miniAppCommandBoundary.setCommandId(new CommandId(miniAppCommandBoundary.getCommandId().getSuperapp(),miniAppCommandBoundary.getCommandId().getMiniapp(),miniAppCommandBoundary.getCommandId().getInternalCommandId()));
        miniAppCommandBoundary.setInvocationTimestamp(miniAppCommandBoundary.getInvocationTimestamp());
        if (miniAppCommandBoundary.getCommandAttributes() == null) {
            miniAppCommandBoundary.setCommandAttributes(new HashMap<>());
        }
        miniAppCommandBoundary.getCommandAttributes().put("status", "in-process...");
        miniAppCommandBoundary.setCommand(miniAppCommandBoundary.getCommand());
        miniAppCommandBoundary.setTargetObject(new TargetObject(miniAppCommandBoundary.getTargetObject().getObjectId()));
        miniAppCommandBoundary.setInvokedBy(new InvokedBy(miniAppCommandBoundary.getInvokedBy().getUserId()));

        try {
            String json = this.jackson
                    .writeValueAsString(miniAppCommandBoundary);

            // send json to afekaMessageQueue
            System.err.println("*** sending: " + json);
            this.jmsTemplate
                    .convertAndSend("InvocationMiniAppQueue", json);
            return miniAppCommandBoundary;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    @JmsListener(destination = "InvocationMiniAppQueue")
    public void handleInvokeMiniApp(String json) {
        try {
            System.err.println(
                    this.entityToBoundary(
                            this.mongoTemplate
                                    .save(
                                            this.boundaryToEntity(
                                                    this.setStatus(
                                                            this.jackson
                                                                    .readValue(json, MiniAppCommandBoundary.class),
                                                            "remotely-accepted")))));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private MiniAppCommandBoundary setStatus(MiniAppCommandBoundary miniAppCommandBoundary, String status) {
        if (miniAppCommandBoundary.getCommandAttributes() == null) {
            miniAppCommandBoundary.setCommandAttributes(new HashMap<>());
        }
        miniAppCommandBoundary.getCommandAttributes().put("status", status);

        return miniAppCommandBoundary;
    }

    @Autowired
    public MiniAppCommandServiceRepo(MongoTemplate mongoTemplate,
                                     MiniAppCommandRepository repository, ObjectMapper jackson, UserRepository userRepository) {

        this.repository = repository;
		this.userRepository = userRepository;
        this.mongoTemplate = mongoTemplate;
        this.jackson = jackson;
    }

    @Override
    public MiniAppCommandBoundary invokeCommand(MiniAppCommandBoundary miniAppCommandBoundary) throws RuntimeException {
        try {
            validatMiniappCommand(miniAppCommandBoundary);
        } catch (RuntimeException e) {
            throw new RuntimeException(e.getMessage());
        }

        CommandId commandId = miniAppCommandBoundary.getCommandId();
        if (commandId.getInternalCommandId() == null || commandId.getInternalCommandId().isEmpty()) {
            commandId = new CommandId(springApplicationName, miniAppCommandBoundary.getCommandId().getMiniapp(), UUID.randomUUID().toString());
            miniAppCommandBoundary.setCommandId(commandId);
        }
        if (commandId.getSuperapp() == null || commandId.getSuperapp().isEmpty()) {
            commandId.setSuperapp(springApplicationName);
        }
        if (commandId.getMiniapp() == null || commandId.getMiniapp().isEmpty()) {
            throw new RuntimeException("Need to specify mini-app");
        }

        miniAppCommandBoundary.setInvokedBy(new InvokedBy(new UserId(miniAppCommandBoundary.getInvokedBy().getUserId().getSuperapp(), miniAppCommandBoundary.getInvokedBy().getUserId().getEmail())));
        miniAppCommandBoundary.setTargetObject(new TargetObject(new ObjectId(miniAppCommandBoundary.getTargetObject().getObjectId().getSuperapp(), miniAppCommandBoundary.getTargetObject().getObjectId().getInternalObjectId())));
        if (miniAppCommandBoundary.getInvocationTimestamp() == null) {
            miniAppCommandBoundary.setInvocationTimestamp(new Date());
        }
        if (miniAppCommandBoundary.getCommandAttributes() == null) {
            miniAppCommandBoundary.setCommandAttributes(new HashMap<>());
        }

        MiniAppCommandEntity entity = boundaryToEntity(miniAppCommandBoundary);
        entity = this.repository.save(entity);
        return this.entityToBoundary(entity);
    }



    private void validatMiniappCommand(MiniAppCommandBoundary miniAppCommandBoundary) throws RuntimeException {
        GeneralUtility generalUtility = new GeneralUtility();
        if (miniAppCommandBoundary.getCommandAttributes() == null) {
            throw new RuntimeException("Command attributes are missing");
        }
        if (generalUtility.isStringEmptyOrNull(miniAppCommandBoundary.getCommand())) {
            throw new RuntimeException("Command details are missing");
        }
        if (generalUtility.isStringEmptyOrNull(miniAppCommandBoundary.getInvokedBy().getUserId().getSuperapp()) ||
        generalUtility.isStringEmptyOrNull(miniAppCommandBoundary.getInvokedBy().getUserId().getEmail())) {
            throw new RuntimeException("Invoked by is missing");
        }
        if (generalUtility.isStringEmptyOrNull(miniAppCommandBoundary.getTargetObject().getObjectId().getInternalObjectId()) ||
                generalUtility.isStringEmptyOrNull(miniAppCommandBoundary.getTargetObject().getObjectId().getSuperapp())) {
            throw new RuntimeException("Target object is missing");
        }
    }


    @Override
    @Deprecated
    public void deleteAllCommands() throws RuntimeException {
    	throw new DepreacatedOpterationException("do not use this operation any more, as it is deprecated");
    }
    
    @Override
    public void deleteAllCommands(UserId userId) {
    	UserEntity userEntity = this.userRepository.findById(userId)
				.orElseThrow(()->new UserNotFoundException("inserted id: " 
    	+ userId.toString() + " does not exist"));
    	
    	if (userEntity.getRole() != UserRole.ADMIN) {
    		throw new PermissionDeniedException("You do not have permission to delete all commands");
    	}
    	this.repository.deleteAll();
    }

    @Override
    public List<MiniAppCommandBoundary> getAllCommands() throws RuntimeException {
        List<MiniAppCommandEntity> entities = this.repository.findAll();
        if (entities.isEmpty()){
            throw new RuntimeException("there aren't any commands");
        }
        List<MiniAppCommandBoundary> boundaries = new ArrayList<>();
        for (MiniAppCommandEntity entity : entities) {
            boundaries.add(this.entityToBoundary(entity));
        }
        return boundaries;
    }

    @Override
    public List<MiniAppCommandBoundary> getAllMiniAppCommands(String miniAppName) throws RuntimeException {
        List<MiniAppCommandEntity> entities = repository.findAllByCommandIdMiniapp(miniAppName);
        if (entities.isEmpty()){
            throw new RuntimeException("mini app history is empty");
        }
        return entities.stream()
                .map(this::entityToBoundary)
                .collect(Collectors.toList());
    }



    public MiniAppCommandBoundary entityToBoundary(MiniAppCommandEntity entity) {
        MiniAppCommandBoundary boundary = new MiniAppCommandBoundary();

        boundary.setCommandId(entity.getCommandId());
        boundary.setCommandAttributes(entity.getCommandAttributes());
        boundary.setCommand(entity.getCommand());
        boundary.setTargetObject(entity.getTargetObject());
        boundary.setInvocationTimestamp(entity.getInvocationTimestamp());
        boundary.setInvokedBy(entity.getInvokedBy());

        return boundary;
    }


    public  MiniAppCommandEntity boundaryToEntity(MiniAppCommandBoundary obj)
    {
        MiniAppCommandEntity entity = new MiniAppCommandEntity();

        entity.setCommand(obj.getCommand());
        if (obj.getCommandId() == null) {
            entity.setCommandId(new CommandId());
        }
        else
            entity.setCommandId(obj.getCommandId());

        if (obj.getCommand() == null) {
            entity.setCommand("");
        }
        else
            entity.setCommand(obj.getCommand());


        if (obj.getTargetObject() == null) {
            entity.setTargetObject(new TargetObject(new ObjectId()));
        }
        else
            entity.setTargetObject(obj.getTargetObject());


        if (obj.getInvokedBy() == null) {
            entity.setInvokedBy(new InvokedBy());
        }else {
            entity.setInvokedBy(obj.getInvokedBy());
        }
        if (obj.getCommandAttributes() == null){
            entity.setCommandAttributes(new HashMap<>() {
            });
        }else {
            entity.setCommandAttributes(obj.getCommandAttributes());
        }

        //Date
        entity.setInvocationTimestamp(obj.getInvocationTimestamp());

        //Data
        entity.setCommandAttributes(obj.getCommandAttributes());

        return entity;
    }
}
