package br.ufes.inf.goophubv2.Controller;

import br.ufes.inf.goophubv2.FileConverter;
import com.complexible.stardog.ext.spring.SnarlTemplate;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.reasoner.structural.StructuralReasonerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

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
                         @RequestParam(value="organization") String organization, @RequestParam(value="role") String role,
                         @RequestParam(value="goal[]") String[] goal,  @RequestParam("file")MultipartFile[] files) {

        FileConverter converter = new FileConverter();
        String result = "";
        try {

            System.out.println("Upload Request:");
            System.out.println("\tName: " + name + "\tEmail: " + email);
            System.out.println("\tOrganization: " + organization + "\tRole: " + role);

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

            byte[] fileContent = files[0].getBytes();
            String s = new String(fileContent);

            System.out.println(s);
            result = converter.convertOWLtoGoop(s, role, goalNames.toString(), "bla");
            Thread.sleep(5000);
        }
        catch (Exception e) {
            return ("Upload error: " + e.getMessage() + " - " + result);
        }
        finally {

            // Add file to DataBase
            snarlTemplate.execute(connection -> {
                try{
                    connection.add().io().file(Paths.get("/home/gabriel/Downloads/goophub-v2/src/main/resources/temp.rdf"));
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                finally {

                    return true;
                }

            });
            return result + "! Goop uploaded";
        }
    }

    @RequestMapping(value = "/atomicupload", method = RequestMethod.POST)
    @ResponseBody
    public String uploadAtomicFile(@RequestParam(value="name") String name, @RequestParam(value="email") String email,
                             @RequestParam(value="organization") String organization, @RequestParam(value="role") String role,
                             @RequestParam(value="goal") String goal, @RequestParam(value="description") String goalDescription,  @RequestParam("file")MultipartFile[] files) {

        FileConverter converter = new FileConverter();
        String result = "";
        try {

            System.out.println("Upload Request:");
            System.out.println("\tName: " + name + "\tEmail: " + email);
            System.out.println("\tOrganization: " + organization + "\tRole: " + role);

            StringBuilder filesNames = new StringBuilder();

            int i;

            for (MultipartFile file : files) {
                Path fileNamePath = Paths.get(uploadDirectory, file.getOriginalFilename());
                filesNames.append(file.getOriginalFilename() + " ");
                try {
                    Files.write(fileNamePath, file.getBytes());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("\tGoal: " + goal);
            System.out.println("\tDescription: " + goalDescription);
            System.out.println("\tFile: "+ filesNames.toString());

            byte[] fileContent = files[0].getBytes();
            String s = new String(fileContent);

            System.out.println(s);
            result = converter.convertOWLtoGoop(s, role, goal.replace(" ", "_"), goalDescription);
            Thread.sleep(5000);
        }
        catch (Exception e) {
            return ("Upload error: " + e.getMessage() + " - " + result);
        }
        finally {

            // Add file to DataBase
            snarlTemplate.execute(connection -> {
                try{
                    connection.add().io().file(Paths.get("/home/gabriel/Downloads/goophub-v2/src/main/resources/temp.rdf"));
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                finally {

                    return true;
                }

            });
            return result + "! Goop uploaded";
        }
    }
}