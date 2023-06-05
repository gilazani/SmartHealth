package superapp.miniapps.commands.dietitiansHelper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import superapp.Boundary.MiniAppCommandBoundary;
import superapp.Boundary.ObjectId;
import superapp.dal.SuperAppObjectCrud;
import superapp.data.SuperAppObjectEntity;
import superapp.logic.utilitys.SuperAppObjectUtility;
import superapp.miniapps.commands.Command;

@Component("GET_ALL_RECIPES")
public class GetAllRecipeCommand implements Command {
    //private final  SpoonaculerService spoonaculerService;
    private SuperAppObjectUtility superAppObjectUtility;

    private SuperAppObjectCrud objectRepository;

    @Autowired
    public GetAllRecipeCommand(SuperAppObjectCrud objectRepository) {
        //  this.spoonaculerService = spoonaculerService;
        this.objectRepository = objectRepository;
        this.superAppObjectUtility = new SuperAppObjectUtility(objectRepository);
    }

    @Override
    public Object execute(MiniAppCommandBoundary miniAppCommandBoundary) {
        // 1. find the dietitian object
        // 2. add new recipe to the dietitian object
        ObjectId idObject = miniAppCommandBoundary.getTargetObject().getObjectId();
        SuperAppObjectEntity dietitian = superAppObjectUtility.checkSuperAppObjectEntityExist(idObject);
        dietitian.getObjectDetails().get(miniAppCommandBoundary.getCommandAttributes().get("recipeName").toString());
        objectRepository.save(dietitian);

        //entity to boundary
        return dietitian;
    }
}
