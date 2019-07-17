package br.ufes.inf.goophubv2.Controller;

import br.ufes.inf.goophubv2.FileConverter;
import com.complexible.stardog.ext.spring.SnarlTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

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

    @RequestMapping(value = "/complexupload", method = RequestMethod.POST)
    @ResponseBody
    public String uploadfile(@RequestParam(value="name") String name, @RequestParam(value="email") String email,
                         @RequestParam(value="organization") String organization, @RequestParam(value="role") String role,
                         @RequestParam(value="goal") String goal, @RequestParam(value="atomics[]") String[] atomicGoals,
                         @RequestParam(value="decomposition") String decomposition, @RequestParam("file")MultipartFile[] files) {

        FileConverter converter = new FileConverter();
        String result = "";
        try {

            System.out.println("Upload Request:");
            System.out.println("\tName: " + name + "\tEmail: " + email);
            System.out.println("\tOrganization: " + organization + "\tRole: " + role);

            StringBuilder filesNames = new StringBuilder();
            String goalNames = "";

            if(goal.isEmpty() || (files.length == 0 || files.length > 1)) {
                Exception e = new Exception("Invalid Form");
                throw e;
            }

            int i;
            for (i = 0; i < atomicGoals.length; i++) {
                goalNames += atomicGoals[i] + ",";
            }
            goalNames = goalNames.substring(0, goalNames.length()-1);

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
            System.out.println("\tGoal Decomposition: " + decomposition);
            System.out.println("\tAtomic Goals: " + goalNames);
            System.out.println("\tFile: "+ filesNames.toString());

            byte[] fileContent = files[0].getBytes();
            String s = new String(fileContent);

            //System.out.println(s);
            result = converter.convertOWLtoGoopComplex(s, role, goal.replace(" ", "_"), decomposition, goalNames.toString());
            Thread.sleep(5000);
            return snarlTemplate.execute(connection -> {
                try{
                    connection.add().io().file(Paths.get("/home/gabriel/Downloads/goophub-v2/src/main/resources/temp.rdf"));
                    return "Upload Complete";
                }
                catch (Exception e) {
                    return ("Upload error: " + e.getMessage());
                }
            });
        }
        catch (Exception e) {
            return ("Upload error: " + e.getMessage());
        }
    }

    @RequestMapping(value = "/atomicupload", method = RequestMethod.POST)
    @ResponseBody
    public String uploadAtomicFile(@RequestParam(value="name") String name, @RequestParam(value="email") String email,
                             @RequestParam(value="organization") String organization, @RequestParam(value="role") String role,
                             @RequestParam(value="goal") String goal,  @RequestParam("file")MultipartFile[] files) {

        FileConverter converter = new FileConverter();
        String result = "";
        try {

            System.out.println("Upload Request:");
            System.out.println("\tName: " + name + "\tEmail: " + email);
            System.out.println("\tOrganization: " + organization + "\tRole: " + role);

            if(goal.isEmpty() || (files.length == 0 || files.length > 1)) {
                Exception e = new Exception("Invalid Form");
                throw e;
            }

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
            System.out.println("\tFile: "+ filesNames.toString());

            byte[] fileContent = files[0].getBytes();
            String s = new String(fileContent);

            result = converter.convertOWLtoGoopAtomic(s, role, goal.replace(" ", "_"));
            Thread.sleep(5000);
            // Add file to DataBase
            return snarlTemplate.execute(connection -> {
                try{
                    connection.add().io().file(Paths.get("/home/gabriel/Downloads/goophub-v2/src/main/resources/temp.rdf"));
                    return "Upload Complete";
                }
                catch (Exception e) {
                    return ("Upload error: " + e.getMessage());
                }
            });
        }
        catch (Exception e) {
            return ("Upload error: " + e.getMessage());
        }
    }
}