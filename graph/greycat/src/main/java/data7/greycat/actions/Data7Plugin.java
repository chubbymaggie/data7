package data7.greycat.actions;

import greycat.Graph;
import greycat.Type;
import greycat.plugin.Plugin;

import static data7.greycat.actions.Data7Actions.*;

public class Data7Plugin implements Plugin {
    @Override
    public void start(Graph graph) {
        graph.actionRegistry()
                .getOrCreateDeclaration(ActionGetVulnerability.NAME)
                .setParams(Type.STRING)
                .setDescription("Get the node corresponding to a vulnerability")
                .setFactory(params -> getVulnerabilityNode((String) params[0])
                );

        graph.actionRegistry()
                .getOrCreateDeclaration(ActionGetOrCreateProject.NAME)
                .setParams(Type.STRING)
                .setDescription("Get the node corresponding to a project")
                .setFactory(params -> getProjectNode((String) params[0]));

        graph.actionRegistry()
                .getOrCreateDeclaration(ActionGetOrCreateFile.NAME)
                .setParams(Type.STRING)
                .setDescription("Get a file Node from a project in the result")
                .setFactory(params -> getFileNode((String) params[0]));
    }

    @Override
    public void stop() {

    }
}
