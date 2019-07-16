package br.ufes.inf.goophubv2.Controller;

import com.complexible.common.base.CloseableIterator;
import com.complexible.stardog.StardogException;
import com.complexible.stardog.api.SelectQuery;
import com.complexible.stardog.api.reasoning.ReasoningConnection;
import com.complexible.stardog.api.search.SearchConnection;
import com.complexible.stardog.api.search.SearchResult;
import com.complexible.stardog.api.search.SearchResults;
import com.complexible.stardog.api.search.Searcher;
import com.complexible.stardog.ext.spring.SnarlTemplate;
import com.complexible.stardog.ext.spring.mapper.SimpleRowMapper;
import com.complexible.stardog.reasoning.ProofWriter;
import com.complexible.stardog.reasoning.StardogExplainer;
import com.stardog.stark.Literal;
import com.stardog.stark.Values;

import com.stardog.stark.query.BindingSet;
import com.stardog.stark.query.SelectQueryResult;
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

    @RequestMapping(value = "/sparql{query}", method = RequestMethod.GET)
    @ResponseBody
    public String sparql(@RequestParam(value="query") String query) {
        String resultJSON = "[";
        String eachResult = "";
        if(query.isEmpty()) {
            return "{\"error\" : \"Query Empty\"}";
        }
        // Queries the database using the SnarlTemplate and gets back a list of mapped objects
        List<Map<String, String>> results = snarlTemplate.query(query, new SimpleRowMapper());
        String[] element;
        if(!results.isEmpty()) {
            for (int i = 0; i < results.size(); i++) {
                //element = results.get(i).toString().split(",");
                //System.out.println(element[0] + " - " + element[1] + " - " + element[2]);
                eachResult = results.get(i).toString().replace("\"", "");
                eachResult = eachResult.replace("=", "\":\"");
                eachResult = eachResult.replace("{", "{\"");
                eachResult = eachResult.replace("}", "\"}");
                eachResult = eachResult.replace(", ", "\", \"");
                eachResult = eachResult.replace("^^", "");
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
    public String goopSearch(@RequestParam(value="query") String query) {
        System.out.println("Query request with param: " + query);
        // Full text search has the ability to do exactly that. Search the database for a specific value.
        // Here we will specify that we only want results over a score of `0.5`, and no more than `2` results
        // for things that match the search term `man`. Below we will perform the search in two different ways.
        String finalResult;
        snarlTemplate.setReasoning(false);
        finalResult = snarlTemplate.execute(connection -> {
            try {

                // The SPARQL syntax is based on the LARQ syntax in Jena.  Here you will
                // see the SPARQL query that is equivalent to the search we just did via `Searcher`,
                // which we can see when we print the results.
                String aQuery = "SELECT DISTINCT ?s WHERE {\n" +
                            "\t?s rdfs:label ?o .\n" +
                            "\t(?o ?score) <" + SearchConnection.MATCH_PREDICATE + "> ( \"" + query + "\" 0.5 2 ).\n" +
                        "}";

                SelectQuery queryMatch = connection
                        .select(aQuery);

                String resultJSON = "{\"goops\": [";
                String iriJSON = "\"iri\": [";
                String eachIRI = "";
                String eachResult = "";
                String[] element;

                try (SelectQueryResult aResult = queryMatch.execute()) {
                    System.out.println("Query results: ");
                    while (aResult.hasNext()) {
                        BindingSet result = aResult.next();
                        if(aResult.hasNext()) {
                            element = result.get("s").toString().split("#");
                            eachResult = "\"" + element[1].replace("_", " ") + "\",";
                            eachIRI = "\"" + result.get("s").toString() + "\",";
                            iriJSON += eachIRI;
                            resultJSON += eachResult;
                        }
                        else {
                            element = result.get("s").toString().split("#");
                            eachIRI = "\"" + result.get("s").toString() + "\"";
                            eachResult = "\"" + element[1].replace("_", " ") + "\"";
                            iriJSON += eachIRI;
                            resultJSON += eachResult;
                        }
                    }
                    iriJSON += "]}";
                    resultJSON += "], " + iriJSON;
                    System.out.println("\t" + resultJSON);
                    return resultJSON;
                }
            } catch (StardogException e) {
                System.out.println("Error with full text search: " + e);
                return "Error with full text search: " + e;
            }
        });
        return finalResult;
    }
}
