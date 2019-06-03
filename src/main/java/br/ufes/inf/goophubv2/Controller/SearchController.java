package br.ufes.inf.goophubv2.Controller;

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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/tool")
public class SearchController {

    @Autowired
    public SnarlTemplate snarlTemplate;


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

    @RequestMapping(value = "/sparql{query}", method = RequestMethod.GET)
    @ResponseBody
    public String sparql(@RequestParam(value="query") String query) {
        String resultJSON = "[";
        String eachResult = "";
        if(query.isEmpty()) {
            query = "PREFIX foaf:<http://xmlns.com/foaf/0.1/> " +
                    "select * { ?s rdf:type foaf:Person }";
        }
        // Queries the database using the SnarlTemplate and gets back a list of mapped objects
        List<Map<String, String>> results = snarlTemplate.query(query, new SimpleRowMapper());
        if(!results.isEmpty()) {
            for (int i = 0; i < results.size(); i++) {
                eachResult = results.get(i).toString().replace("=", "\":\"");
                eachResult = eachResult.replace("{", "{\"");
                eachResult = eachResult.replace("}", "\"}");
                eachResult = eachResult.replace(", ", "\", \"");
                if(results.size() == 1) {
                    resultJSON += eachResult;
                }
                if(i != results.size()-1) {
                    if (i == 0)
                        resultJSON += eachResult;
                    else
                        resultJSON += ", " + eachResult;
                }
            }
        }
        resultJSON = resultJSON + "]";
        return resultJSON;
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
                    System.out.println("\t" + resultJSON);
                    return resultJSON;
                }
            } catch (StardogException e) {
                System.out.println("Error with full text search: " + e);
                return "{\"goop\": \"Query Error\"}";
            }
        });
        return finalResult;
    }


}
