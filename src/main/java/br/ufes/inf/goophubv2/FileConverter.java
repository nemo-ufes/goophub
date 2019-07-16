package br.ufes.inf.goophubv2;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.StringDocumentSource;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.reasoner.structural.StructuralReasonerFactory;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class FileConverter {

    public String convertOWLtoGoop(String file, String actorName, String goalId, String goalDescription) {

        try {
            // Creating Ontology Manager
            OWLOntologyManager manager = OWLManager.createOWLOntologyManager();

            // Loading GOOP Metamodel
            OWLOntology goop = manager.loadOntologyFromOntologyDocument(new File("/home/gabriel/Downloads/GOOP/goop-meta-model.owl"));

            // Loading source ontology and creating a resource factory
            OWLOntology ontologiaAlvo = manager.loadOntologyFromOntologyDocument(new StringDocumentSource(file));
            String sourceIRI = ontologiaAlvo.getOntologyID().getOntologyIRI().get().toString() + "#";
            OWLDataFactory factory = OWLManager.getOWLDataFactory();

            // Reasoner
            OWLReasonerFactory reasonerFactory = new StructuralReasonerFactory();
            OWLReasoner reasoner = reasonerFactory.createReasoner(ontologiaAlvo);
            reasoner.precomputeInferences();

            // Utils
            OWLIndividual clss;
            OWLIndividual subClss;
            OWLIndividual objectProperty;
            OWLIndividual actor;
            OWLIndividual goal;
            List<OWLIndividual> classIndividualsList = new LinkedList<OWLIndividual>();

            String actorRole = actorName;
            String goalName = goalId;

            // Classes needed for GOOPs
            OWLClass goopType = factory.getOWLClass(IRI.create(goop.getOntologyID().getOntologyIRI().get() + "#Goop"));
            OWLClass classType = factory.getOWLClass(IRI.create(goop.getOntologyID().getOntologyIRI().get() + "#owl:Class"));
            OWLClass objType = factory.getOWLClass(IRI.create(goop.getOntologyID().getOntologyIRI().get() + "#owl:ObjectProperty"));
            OWLClass actorType = factory.getOWLClass(IRI.create(goop.getOntologyID().getOntologyIRI().get() + "#Actor"));
            OWLClass goalType = factory.getOWLClass(IRI.create(goop.getOntologyID().getOntologyIRI().get() + "#Goal"));

            // Object Properties needed for GOOP
            OWLObjectProperty subClassOf = factory.getOWLObjectProperty("http://www.w3.org/2002/07/owl#subClassOf");
            OWLObjectProperty composedBy = factory.getOWLObjectProperty("https://nemo.inf.ufes.br/dev/ontology/Goop#composed_by");
            OWLObjectProperty achievesGoalOf = factory.getOWLObjectProperty("https://nemo.inf.ufes.br/dev/ontology/Goop#achieves_goal_of");
            OWLObjectProperty usedToAchieve = factory.getOWLObjectProperty("https://nemo.inf.ufes.br/dev/ontology/Goop#used_to_achieve");
            OWLObjectProperty has = factory.getOWLObjectProperty("https://nemo.inf.ufes.br/dev/ontology/Goop#has");
            OWLObjectProperty domain = factory.getOWLObjectProperty("http://www.w3.org/2002/07/owl#domain");
            OWLObjectProperty range = factory.getOWLObjectProperty("http://www.w3.org/2002/07/owl#range");

            // Variables to label entities
            OWLLiteral lbl;
            OWLAnnotation label;
            OWLAxiom labelAxiom;


            OWLClassAssertionAxiom classAxiom;

            // Creating GOOP of source ontology
            OWLIndividual placeGoop = factory.getOWLNamedIndividual(IRI.create(goop.getOntologyID().getOntologyIRI().get() + "#Goop"));
            classAxiom = factory.getOWLClassAssertionAxiom(goopType, placeGoop);
            manager.applyChange(new AddAxiom(goop, classAxiom));

            actor = factory.getOWLNamedIndividual(IRI.create(goop.getOntologyID().getOntologyIRI().get() + "#" + actorRole));

            // Add Label to Actor
            lbl = factory.getOWLLiteral(actorName);
            label = factory.getOWLAnnotation(factory.getOWLAnnotationProperty(OWLRDFVocabulary.RDFS_LABEL.getIRI()), lbl);
            labelAxiom = factory.getOWLAnnotationAssertionAxiom(actor.asOWLNamedIndividual().getIRI(), label);
            manager.applyChange(new AddAxiom(goop, labelAxiom));

            classAxiom = factory.getOWLClassAssertionAxiom(actorType, actor);
            manager.applyChange(new AddAxiom(goop, classAxiom));

            goal = factory.getOWLNamedIndividual(IRI.create(goop.getOntologyID().getOntologyIRI().get() + "#" + goalName));
            classAxiom = factory.getOWLClassAssertionAxiom(goalType, goal);
            manager.applyChange(new AddAxiom(goop, classAxiom));

            // Add Label to Goal
            lbl = factory.getOWLLiteral(goalName.replace("_", " "));
            label = factory.getOWLAnnotation(factory.getOWLAnnotationProperty(OWLRDFVocabulary.RDFS_LABEL.getIRI()), lbl);
            labelAxiom = factory.getOWLAnnotationAssertionAxiom(goal.asOWLNamedIndividual().getIRI(), label);
            manager.applyChange(new AddAxiom(goop, labelAxiom));

            // Add Goal Description
            lbl = factory.getOWLLiteral(goalDescription.replace("_", " "));
            label = factory.getOWLAnnotation(factory.getOWLAnnotationProperty(OWLRDFVocabulary.RDF_DESCRIPTION.getIRI()), lbl);
            labelAxiom = factory.getOWLAnnotationAssertionAxiom(goal.asOWLNamedIndividual().getIRI(), label);
            manager.applyChange(new AddAxiom(goop, labelAxiom));

            System.out.println("----- Retrieving All Classes of " + ontologiaAlvo.getOntologyID().getOntologyIRI().get());

            Set<OWLClass> subClses;
            Set<OWLClass> placeClasses = ontologiaAlvo.getClassesInSignature();
            for (OWLClass iter : placeClasses) {
                System.out.println(iter.toString());

                String iterName = iter.toString().replace("<", "").replace(">", "");

                clss = factory.getOWLNamedIndividual(iterName);
                classAxiom = factory.getOWLClassAssertionAxiom(classType, clss);
                AddAxiom addClassAxiom = new AddAxiom(goop, classAxiom);

                // Add label to Classes
                lbl = factory.getOWLLiteral(((OWLNamedIndividual) clss).getIRI().getFragment());
                label = factory.getOWLAnnotation(factory.getOWLAnnotationProperty(OWLRDFVocabulary.RDFS_LABEL.getIRI()), lbl);
                labelAxiom = factory.getOWLAnnotationAssertionAxiom(clss.asOWLNamedIndividual().getIRI(), label);
                manager.applyChange(new AddAxiom(goop, labelAxiom));

                manager.applyChange(addClassAxiom);

                classIndividualsList.add(clss);

                // Reasoning over direct subClasses
                subClses = reasoner.getSubClasses(iter, true).getFlattened();
                for (OWLClass iter2 : subClses) {
                    System.out.println("\t" + iter2.toString());
                    String iter2Name = iter2.toString().replace("<", "").replace(">", "");
                    subClss = factory.getOWLNamedIndividual(iter2Name);
                    if (!iter2.toString().equals("owl:Nothing")) {
                        OWLObjectPropertyAssertionAxiom subClassOfAxiom = factory.getOWLObjectPropertyAssertionAxiom(subClassOf, subClss, clss);
                        manager.applyChange(new AddAxiom(goop, subClassOfAxiom));
                        // Add label to Classes
                        lbl = factory.getOWLLiteral(((OWLNamedIndividual) subClss).getIRI().getFragment());
                        label = factory.getOWLAnnotation(factory.getOWLAnnotationProperty(OWLRDFVocabulary.RDFS_LABEL.getIRI()), lbl);
                        labelAxiom = factory.getOWLAnnotationAssertionAxiom(subClss.asOWLNamedIndividual().getIRI(), label);
                        manager.applyChange(new AddAxiom(goop, labelAxiom));
                    }
                }
            }

            // Add composedBy relations between Goop -> Classes
            for (OWLIndividual iter : classIndividualsList) {
                OWLObjectPropertyAssertionAxiom composedByAxiom = factory.getOWLObjectPropertyAssertionAxiom(composedBy, placeGoop, iter);
                manager.applyChange(new AddAxiom(goop, composedByAxiom));
            }

            System.out.println("----- Retrieving All Object Properties of " + ontologiaAlvo.getOntologyID().getOntologyIRI().get());

            Set<OWLObjectProperty> placeObj = ontologiaAlvo.getObjectPropertiesInSignature();
            for (OWLObjectProperty iter : placeObj) {
                System.out.println(iter.toString());
                String iterName = iter.toString().replace("<", "").replace(">", "");

                objectProperty = factory.getOWLNamedIndividual(iterName);
                classAxiom = factory.getOWLClassAssertionAxiom(objType, objectProperty);
                AddAxiom addClassAxiom = new AddAxiom(goop, classAxiom);
                manager.applyChange(addClassAxiom);

                // Add label to ObjectProperties
                lbl = factory.getOWLLiteral(((OWLNamedIndividual) objectProperty).getIRI().getFragment());
                label = factory.getOWLAnnotation(factory.getOWLAnnotationProperty(OWLRDFVocabulary.RDFS_LABEL.getIRI()), lbl);
                labelAxiom = factory.getOWLAnnotationAssertionAxiom(objectProperty.asOWLNamedIndividual().getIRI(), label);
                manager.applyChange(new AddAxiom(goop, labelAxiom));

                OWLObjectPropertyAssertionAxiom composedByAxiom = factory.getOWLObjectPropertyAssertionAxiom(composedBy, placeGoop, objectProperty);
                manager.applyChange(new AddAxiom(goop, composedByAxiom));

                // Defining the domain of each property
                Set<OWLClass> domainSet = reasoner.getObjectPropertyDomains(iter, true).getFlattened();
                for (OWLClass iter2 : domainSet) {
                    for (OWLIndividual iter3 : classIndividualsList) {
                        String iter3Name = iter3.toString().replace("<", "").replace(">", "");
                        String iter2Name = iter2.toString().replace("<", "").replace(">", "");
                        if (iter3Name.equals(iter2Name)) {
                            OWLObjectPropertyAssertionAxiom domainAxiom = factory.getOWLObjectPropertyAssertionAxiom(domain, objectProperty, iter3);
                            manager.applyChange(new AddAxiom(goop, domainAxiom));
                        }
                    }
                }

                // Defining the range of each property
                Set<OWLClass> rangeSet = reasoner.getObjectPropertyRanges(iter, true).getFlattened();
                for (OWLClass iter2 : domainSet) {
                    for (OWLIndividual iter3 : classIndividualsList) {
                        String iter3Name = iter3.toString().replace("<", "").replace(">", "");
                        String iter2Name = iter2.toString().replace("<", "").replace(">", "");
                        if (iter3Name.equals(iter2Name)) {
                            OWLObjectPropertyAssertionAxiom rangeAxiom = factory.getOWLObjectPropertyAssertionAxiom(range, objectProperty, iter3);
                            manager.applyChange(new AddAxiom(goop, rangeAxiom));
                        }
                    }
                }
            }

            // Add composedBy relations between Goop -> Classes
            for (OWLIndividual iter : classIndividualsList) {
                OWLObjectPropertyAssertionAxiom composedByAxiom = factory.getOWLObjectPropertyAssertionAxiom(composedBy, placeGoop, iter);
                manager.applyChange(new AddAxiom(goop, composedByAxiom));
            }

            OWLObjectPropertyAssertionAxiom achievesAxiom = factory.getOWLObjectPropertyAssertionAxiom(achievesGoalOf, placeGoop, actor);
            manager.applyChange(new AddAxiom(goop, achievesAxiom));

            OWLObjectPropertyAssertionAxiom goalAxiom = factory.getOWLObjectPropertyAssertionAxiom(usedToAchieve, placeGoop, goal);
            manager.applyChange(new AddAxiom(goop, goalAxiom));

            OWLObjectPropertyAssertionAxiom hasAxiom = factory.getOWLObjectPropertyAssertionAxiom(has, actor, goal);
            manager.applyChange(new AddAxiom(goop, hasAxiom));

            File fileformated = new File("/home/gabriel/Downloads/goophub-v2/src/main/resources/temp.rdf");

            System.out.println("RDF/XML: ");
            manager.saveOntology(goop, IRI.create(fileformated.toURI()));
        } catch (Exception e) {
            return e.getMessage();
        }
        finally {
            return "Success";
        }
    }
}
