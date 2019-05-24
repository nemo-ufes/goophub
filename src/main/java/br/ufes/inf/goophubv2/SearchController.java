package br.ufes.inf.goophubv2;

import com.complexible.common.base.CloseableIterator;
import com.complexible.stardog.StardogException;
import com.complexible.stardog.api.reasoning.ReasoningConnection;
import com.complexible.stardog.api.search.SearchConnection;
import com.complexible.stardog.api.search.SearchResult;
import com.complexible.stardog.api.search.SearchResults;
import com.complexible.stardog.api.search.Searcher;
import com.complexible.stardog.ext.spring.SnarlTemplate;
import com.complexible.stardog.ext.spring.mapper.SimpleRowMapper;
import com.complexible.stardog.reasoning.ProofWriter;
import com.complexible.stardog.reasoning.StardogExplainer;
import com.stardog.stark.Values;
import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntProperty;
import org.apache.jena.rdf.model.*;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.RDFS;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/")
public class SearchController {

    @Autowired
    public SnarlTemplate snarlTemplate;

    // Page Routes
    @RequestMapping(value = "/search", method = RequestMethod.GET)
    public String searchPage() { return "search"; }

    @RequestMapping(value = "/index", method = RequestMethod.GET)
    public String indexPage() { return "index"; }

    @RequestMapping(value = "/endpoint", method = RequestMethod.GET)
    public String endpointPage() {
        return "endpoint";
    }

    @RequestMapping(value = "/upload", method = RequestMethod.GET)
    public String uploadPage() {
        return "upload";
    }

    @RequestMapping(value = "/joint", method = RequestMethod.GET)
    public String jointPage() { return "joint"; }

    // Functionalities Routes
    @RequestMapping("/searchTest")
    public void search () {
        // Query to run
        String sparql = "PREFIX foaf:<http://xmlns.com/foaf/0.1/> " +
                "select * { ?s rdf:type foaf:Person }";

        // Queries the database using the SnarlTemplate and gets back a list of mapped objects
        List<Map<String, String>> results = snarlTemplate.query(sparql, new SimpleRowMapper());

        // Prints out the results
        System.out.println("** Members of Marvel Universe **");
        results.forEach(item -> item.forEach((k,v) -> System.out.println(v)));
    }

    @RequestMapping("/reasoning")
    public String reasoning() {
        System.out.println("\n** Ask the database questions **");

        // Using the SnarlTemplate, you can ask the database questions. In the ontology, just because
        // you know someones does not imply that they know you.
        String askQuery = " PREFIX foaf:<http://xmlns.com/foaf/0.1/>" +
                "ASK { :ironMan foaf:knows :spiderMan } ";
        System.out.println("Does Iron Man know Spiderman? " + snarlTemplate.ask(askQuery));

        askQuery = " PREFIX foaf:<http://xmlns.com/foaf/0.1/>" +
                "ASK { :spiderMan foaf:knows :ironMan } ";
        System.out.println("Does Spiderman know IronMan? " + snarlTemplate.ask(askQuery));
        return "Reasoning executed";
    }

    @RequestMapping("/ask")
    public String ask() {
        System.out.println("\n** Show Reasoning **");
        // Queries the database with reasoning off. With reasoning off, there would be
        // no connection between Spider-Man and his mother, Mary Parker, since the triple
        // is only associated with his mother
        boolean aExistsNoReasoning = snarlTemplate
                .reasoning(false)
                .subject(Values.iri("http://api.stardog.com/spiderMan"))
                .predicate(Values.iri("http://api.stardog.com/childOf"))
                .object(Values.iri("http://api.stardog.com/maryParker"))
                .ask();


        System.out.println("aExistsNoReasoning: " + aExistsNoReasoning);

        // Queries the database with reasoning on. There is now a connection between
        // Spider-Man and his mother, Mary Parker. This locial connection exists since
        // there is a triple associated with Mary Parker and the ontology says that
        // :childOf is the inverse of :parentOf.
        boolean aExistsReasoning = snarlTemplate
                .reasoning(true)
                .subject(Values.iri("http://api.stardog.com/spiderMan"))
                .predicate(Values.iri("http://api.stardog.com/childOf"))
                .object(Values.iri("http://api.stardog.com/maryParker"))
                .ask();

        System.out.println("aExistsReasoning: " + aExistsReasoning);
        return "Ask executed";
    }

    @RequestMapping("/inference")
    public String inference() {
        // Using reasoning, we can get an explanation on how the inferred connection between the
        // objects was made. Here we are going to send the same query to the reasoning and get
        // an explanation back as to how they were connected and why the results were returned.
        System.out.println("\n** Show Inference **");
        StardogExplainer aExplanation = snarlTemplate
                .as(ReasoningConnection.class)
                .explain(Values.statement(
                        Values.iri("http://api.stardog.com/spiderMan"),
                        Values.iri("http://api.stardog.com/childOf"),
                        Values.iri("http://api.stardog.com/maryParker")));


        System.out.println("Explain inference: ");
        System.out.println(ProofWriter.toString(aExplanation.proof()));
        return "Inference executed";
    }

    @RequestMapping(value = "/query{query}", method = RequestMethod.GET)
    @ResponseBody
    public String search(@RequestParam(value="query") String query) {
        System.out.println("Query request with param: " + query);
        // Full text search has the ability to do exactly that. Search the database for a specific value.
        // Here we will specify that we only want results over a score of `0.5`, and no more than `2` results
        // for things that match the search term `man`. Below we will perform the search in two different ways.
        String finalResult;
        snarlTemplate.setReasoning(false);
        finalResult = snarlTemplate.execute(connection -> {
            try {
                // Stardog's full text search is backed by [Lucene](http://lucene.apache.org)
                // so you can use the full Lucene search syntax in your queries.
                Searcher aSearch = connection
                        .as(SearchConnection.class)
                        .search()
                        .limit(2)
                        .query(query)
                        .threshold(0.5);

                // We can run the search and then iterate over the results
                SearchResults aSearchResults = aSearch.search();

                // Building JSON for response
                String resultJSON = "{\"goops\": [";
                String eachResult = "";
                String[] element;
                try (CloseableIterator<SearchResult> resultIt = aSearchResults.iterator()) {
                    while (resultIt.hasNext()) {
                        SearchResult aHit = resultIt.next();
                        if(resultIt.hasNext()) {
                            element = aHit.getHit().toString().split("#");
                            eachResult = "\"" + element[1] + "\",";
                            resultJSON += eachResult;
                        }
                        else {
                            element = aHit.getHit().toString().split("#");
                            eachResult = "\"" + element[1].replace("_", " ") + "\"";
                            resultJSON += eachResult;
                        }
                    }
                    resultJSON += "]}";
                    System.out.println(resultJSON);
                    return resultJSON;
                }
            } catch (StardogException e) {
                System.out.println("Error with full text search: " + e);
                return "{\"goop\": \"Query Error\"}";
            }
        });
        return finalResult;
    }

    // Upload Files

    @RequestMapping("/api/upload")
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
