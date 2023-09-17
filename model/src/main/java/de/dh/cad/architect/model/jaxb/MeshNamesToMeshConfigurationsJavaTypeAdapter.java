package de.dh.cad.architect.model.jaxb;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;

import de.dh.cad.architect.model.assets.MeshConfiguration;
import de.dh.cad.architect.model.jaxb.MeshNamesToMeshConfigurationsJavaTypeAdapter.MCsProxy;

public class MeshNamesToMeshConfigurationsJavaTypeAdapter extends XmlAdapter<MCsProxy, Map<String, MeshConfiguration>> {
    public static class MCsProxy {
        protected List<MeshConfiguration> mEntries = new ArrayList<>();

        public MCsProxy() {
            // For JAXB
        }

        public MCsProxy(Collection<MeshConfiguration> entries) {
            mEntries.addAll(entries);
        }

        @XmlElement(name = "MeshConfiguration")
        public List<MeshConfiguration> getEntries() {
            return mEntries;
        }
    }

    @Override
    public Map<String, MeshConfiguration> unmarshal(MCsProxy v) throws Exception {
        return v.getEntries().stream().collect(Collectors.toMap(MeshConfiguration::getMeshName, mc -> mc));
    }

    @Override
    public MCsProxy marshal(Map<String, MeshConfiguration> v) throws Exception {
        return new MCsProxy(v.values());
    }
}
