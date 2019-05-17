package br.ufes.inf.goophubv2;

import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntProperty;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.RDFS;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;

import static java.lang.System.out;

public class FileConverter {

    public void RDFtoGopp() {

        try {
            // Reading Goop Meta-Model
            String goopFile = "/home/gabriel/Downloads/goophub-v2/src/main/resources/goop-meta-model.owl";
            String NS = "https://nemo.inf.ufes.br/dev/ontology/Goop#";
            OntModel goopModel = ModelFactory.createOntologyModel();
            goopModel.read(new FileInputStream(goopFile), null);

            // Reading Ontology Fragment
            String fragmentFile = "/home/gabriel/Downloads/goophub-v2/src/main/resources/place_names_root.owl";
            OntModel fragmentModel = ModelFactory.createOntologyModel();
            fragmentModel.read(new FileInputStream(fragmentFile), null);

            // Creating Goop Individual
            OntClass goopClass = goopModel.getOntClass(NS + "Goop");
            Individual goopIndividual = goopModel.createIndividual(NS + "_Goop_", goopClass);

            // Searching for classes
            OntClass owlClass = goopModel.getOntClass(NS + "owl:Class");
            Individual individual;
            for (ExtendedIterator<?> it = fragmentModel.listClasses(); it.hasNext(); ) {
                OntClass p = (OntClass) it.next();
                if (p.hasSubClass()) {
                    for (ExtendedIterator<?> it2 = p.listSubClasses(); it2.hasNext(); ) {
                        OntClass pSub = (OntClass) it2.next();
                        individual = goopModel.createIndividual(NS + pSub.getLocalName(), owlClass);
                        individual.addProperty(RDFS.subClassOf, ResourceFactory.createResource(NS + p.getLocalName()));
                        goopIndividual.addProperty(goopModel.getObjectProperty(NS + "composed_by"), individual);
                    }
                }
            }
            // Searching for ObjectProperty
            OntClass owlObjectProperty = goopModel.getOntClass(NS + "owl:Object_Property");
            for (ExtendedIterator<?> it = fragmentModel.listObjectProperties(); it.hasNext(); ) {
                OntProperty p = (OntProperty) it.next();
                individual = goopModel.createIndividual(NS + p.getLocalName(), owlObjectProperty);
                individual.addProperty(RDFS.domain, ResourceFactory.createResource(NS + p.getDomain().getLocalName()));
                individual.addProperty(RDFS.range, ResourceFactory.createResource(NS + p.getRange().getLocalName()));
                goopIndividual.addProperty(goopModel.getObjectProperty(NS + "composed_by"), individual);
            }

            // Creating Goal and associating with GOOP
            OntClass goalClass = goopModel.getOntClass(NS + "Complex_Goal");
            Individual goalIndividual = goopModel.createIndividual(NS + "Describe_Location", goalClass);
            goopIndividual.addProperty(goopModel.getObjectProperty(NS + "composed_by"), goalIndividual);

            // Creating Actor and associating with GOOP and Goal
            OntClass actorClass = goopModel.getOntClass(NS + "Actor");
            Individual actorIndividual = goopModel.createIndividual(NS + "Researcher", actorClass);
            actorIndividual.addProperty(goopModel.getObjectProperty(NS + "has"), goalIndividual);
            goopIndividual.addProperty(goopModel.getObjectProperty(NS + "achieves_goal_of"), actorIndividual);

            // Generation RDF File
            try {
                String fileName = "classpath:tmp.rdf";
                FileWriter out = new FileWriter(fileName);
                goopModel.write(out);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                out.close();
            }
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}