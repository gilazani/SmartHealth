package superapp.miniapps.commands.dietitiansHelper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import superapp.Boundary.MiniAppCommandBoundary;
import superapp.Boundary.ObjectId;
import superapp.dal.SuperAppObjectCrud;
import superapp.data.SuperAppObjectEntity;
import superapp.logic.utilitys.SuperAppObjectUtility;
import superapp.miniapps.commands.Command;

@Component("DELETE_RECIPE")
public class DeleteRecipeCommand implements Command {
    //private final  SpoonaculerService spoonaculerService;
    private SuperAppObjectUtility superAppObjectUtility;
    private SuperAppObjectCrud objectRepository;

    @Autowired
    public DeleteRecipeCommand(SuperAppObjectCrud objectRepository) {
        //  this.spoonaculerService = spoonaculerService;
        this.objectRepository = objectRepository;
        this.superAppObjectUtility = new SuperAppObjectUtility(objectRepository);
    }

    @Override
    public Object execute(MiniAppCommandBoundary miniAppCommandBoundary) {
        // 1. Find the dietitian object using the provided object ID
        ObjectId idObject = miniAppCommandBoundary.getTargetObject().getObjectId();
        SuperAppObjectEntity dietitian = superAppObjectUtility.checkSuperAppObjectEntityExist(idObject);

        // 2. Remove the recipe from the dietitian object using the recipe name
        dietitian.deleteRecipeFromObjectDetails(miniAppCommandBoundary.getCommandAttributes().get("recipeName").toString());

        // 3. Save the updated dietitian object
        objectRepository.save(dietitian);

        // Convert the remaining recipes to a boundary object and return it
        return dietitian.getObjectDetails().get("recipes");
    }
}
