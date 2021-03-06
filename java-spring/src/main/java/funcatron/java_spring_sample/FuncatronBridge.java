package funcatron.java_spring_sample;

import funcatron.service.spring_boot.SpringBootWrapper;


/**
 * Bridges Spring to Funcation. You must include a file like this in your project as
 * Spring's maven packaging doesn't pick up the services from included projects.
 */

public class FuncatronBridge extends SpringBootWrapper {

    public FuncatronBridge() {
        super();
    }

    @Override
    public Class<?>[] classList() {
        return new Class<?>[]{Application.class};
    }
}
