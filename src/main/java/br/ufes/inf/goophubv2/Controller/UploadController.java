package br.ufes.inf.goophubv2.Controller;

import com.complexible.stardog.ext.spring.SnarlTemplate;
import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntProperty;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.RDFS;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Controller
@RequestMapping("/api")
public class UploadController {

    @Autowired
    SnarlTemplate snarlTemplate;

    public static String uploadDirectory = System.getProperty("user.dir")+"/uploads";

    @RequestMapping("/upload")
    public String uploadPage(Model model) {
        return "uploadview";
    }

    @RequestMapping(value = "/uploadfile", method = RequestMethod.POST)
    @ResponseBody
    public String uploadfile(@RequestParam(value="name") String name, @RequestParam(value="email") String email,
                         @RequestParam(value="institution") String institution, @RequestParam(value="role") String role,
                         @RequestParam(value="goal[]") String[] goal,  @RequestParam("file")MultipartFile[] files) {

        try {
            System.out.println("Upload Request:");
            System.out.println("\tName: " + name + "\tEmail: " + email);
            System.out.println("\tInstitution: " + institution + "\tRole: " + role);

            StringBuilder filesNames = new StringBuilder();
            StringBuilder goalNames = new StringBuilder();

            int i;
            for (i = 0; i < goal.length; i++) {
                goalNames.append(goal[i] + " ");
            }

            for (MultipartFile file : files) {
                Path fileNamePath = Paths.get(uploadDirectory, file.getOriginalFilename());
                filesNames.append(file.getOriginalFilename() + " ");
                try {
                    Files.write(fileNamePath, file.getBytes());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("\tGoals: " + goalNames.toString());
            System.out.println("\tFile: "+ filesNames.toString());
        }
        catch (Exception e) {
            return ("Upload error: " + e.getMessage());
        }
        finally {
            return "Goop uploaded";
        }
    }

    // Upload Files
    @RequestMapping("/converter")
    public void uploadFile() throws IOException {

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
        Individual goopIndividual = goopModel.createIndividual( NS + "_Goop_", goopClass);

        // Searching for classes
        OntClass owlClass = goopModel.getOntClass(NS + "owl:Class");
        Individual individual;
        for (ExtendedIterator<?> it = fragmentModel.listClasses(); it.hasNext(); ) {
            OntClass p = (OntClass) it.next();
            if (p.hasSubClass()) {
                for (ExtendedIterator<?> it2 = p.listSubClasses(); it2.hasNext(); ) {
                    OntClass pSub = (OntClass) it2.next();
                    individual = goopModel.createIndividual( NS + pSub.getLocalName(), owlClass);
                    individual.addProperty(RDFS.subClassOf, ResourceFactory.createResource(NS + p.getLocalName()));
                    individual.setLabel(pSub.getLocalName(), "en");
                    goopIndividual.addProperty(goopModel.getObjectProperty(NS + "composed_by"), individual);
                }
            }
        }
        // Searching for ObjectProperty
        OntClass owlObjectProperty = goopModel.getOntClass(NS + "owl:Object_Property");
        for (ExtendedIterator<?> it = fragmentModel.listObjectProperties(); it.hasNext(); ) {
            OntProperty p = (OntProperty) it.next();
            individual = goopModel.createIndividual( NS + p.getLocalName(), owlObjectProperty);
            individual.addProperty(RDFS.domain, ResourceFactory.createResource(NS + p.getDomain().getLocalName()));
            individual.addProperty(RDFS.range, ResourceFactory.createResource(NS + p.getRange().getLocalName()));
            individual.setLabel(p.getLocalName(), "en");
            goopIndividual.addProperty(goopModel.getObjectProperty(NS + "composed_by"), individual);
        }

        // Creating Goal and associating with GOOP
        OntClass goalClass = goopModel.getOntClass(NS + "Complex_Goal");
        Individual goalIndividual = goopModel.createIndividual( NS + "Describe_Location", goalClass);
        goalIndividual.setLabel("Describe Location", "en");
        goopIndividual.addProperty(goopModel.getObjectProperty(NS + "composed_by"), goalIndividual);

        // Creating Actor and associating with GOOP and Goal
        OntClass actorClass = goopModel.getOntClass(NS + "Actor");
        Individual actorIndividual = goopModel.createIndividual( NS + "Researcher", actorClass);
        actorIndividual.setLabel("Researcher", "en");
        actorIndividual.addProperty(goopModel.getObjectProperty(NS + "has"), goalIndividual);
        goopIndividual.addProperty(goopModel.getObjectProperty(NS + "achieves_goal_of"), actorIndividual);

        // Generation RDF File
        String fileName = "classpath:tmp.rdf";
        FileWriter out = new FileWriter(fileName);
        try {
            goopModel.write(out);
            //goopModel.write(System.out);
        }
        finally {
            try {
                out.close();
            }
            catch (IOException closeException) {
                System.out.println(closeException.getStackTrace());
            }
        }

        // Add file to DataBase
        snarlTemplate.execute(connection -> {
            try{
                connection.add().io().file(Paths.get("classpath:tmp.rdf"));
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            return true;
        });
    }
}