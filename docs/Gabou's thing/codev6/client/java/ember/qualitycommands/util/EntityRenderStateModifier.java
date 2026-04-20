package ember.qualitycommands.util;

import java.util.List;
import java.util.Map;

public abstract interface EntityRenderStateModifier {
    abstract Map<String,List<String>> getTargets();
    abstract void setTargets(Map<String,List<String>> targets);
    abstract List<String> getOverlays();
    abstract void setOverlays(List<String> overlays);
    abstract List<String> getOverlaysE();
    abstract void setOverlaysE(List<String> overlaysE);
}
