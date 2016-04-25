package me.smecsia.gawain.jackson;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.Module;

/**
 * @author Ilya Sadykov
 */
class GawainJacksonModule extends Module {
    @Override
    public String getModuleName() {
        return "selenograph";
    }

    @Override
    public Version version() {
        return new Version(1, 0, 0, null, "ru.qatools.selenograph", "selenograph");
    }

    @Override
    public void setupModule(SetupContext context) {
        context.addDeserializers(new GawainDeserializers());
    }
}
