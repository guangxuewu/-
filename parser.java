import edu.stanford.nlp.dcoref.CorefChain;
import edu.stanford.nlp.dcoref.CorefCoreAnnotations;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.PropertiesUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Properties;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;

public class parser {

    public static void main(String[] args) {

        parser example = new parser();

        example.runChineseAnnotators();

    }

    public void runChineseAnnotators() {
        Integer counter = 0;
        String comma = "";
        
        try {
        // Read Dictionary
        FileReader dictFile = new FileReader("sentimentDict_GBK.csv");
        BufferedReader dictReader = new BufferedReader(dictFile);
        String entry = "";
        HashMap dict = new HashMap();
        while(dictReader.ready()){
            entry = dictReader.readLine();
            String[] parts = entry.split(",");
            dict.put(parts[0], parts[1]);
        }
        dictFile.close();
        // Read ntusd pos dict
        dictFile = new FileReader("../OuterDict/ntusd-positive-GBK.txt");
        dictReader = new BufferedReader(dictFile);
        while(dictReader.ready()){
            entry = dictReader.readLine();
            dict.put(entry, "1");
        }
        dictFile.close();
        // Read ntusd neg dict
        dictFile = new FileReader("../OuterDict/ntusd-negative-GBK.txt");
        dictReader = new BufferedReader(dictFile);
        while(dictReader.ready()){
            entry = dictReader.readLine();
            dict.put(entry, "-1");
        }
        dictFile.close();
        // Read ntusd pos judge dict
        dictFile = new FileReader("../OuterDict/pos-judge.txt");
        dictReader = new BufferedReader(dictFile);
        while(dictReader.ready()){
            entry = dictReader.readLine();
            dict.put(entry, "1");
        }
        dictFile.close();
        // Read ntusd neg judge dict
        dictFile = new FileReader("../OuterDict/neg-judge.txt");
        dictReader = new BufferedReader(dictFile);
        while(dictReader.ready()){
            entry = dictReader.readLine();
            dict.put(entry, "-1");
        }
        dictFile.close();
        // Read ntusd pos sent dict
        dictFile = new FileReader("../OuterDict/pos-sent.txt");
        dictReader = new BufferedReader(dictFile);
        while(dictReader.ready()){
            entry = dictReader.readLine();
            dict.put(entry, "1");
        }
        dictFile.close();
        // Read ntusd neg sent dict
        dictFile = new FileReader("../OuterDict/neg-sent.txt");
        dictReader = new BufferedReader(dictFile);
        while(dictReader.ready()){
            entry = dictReader.readLine();
            dict.put(entry, "-1");
        }
        dictFile.close();

        // Read Chinese Comma
        dictFile = new FileReader("comma_GBK");
        dictReader = new BufferedReader(dictFile);
        while(dictReader.ready()){
            comma = dictReader.readLine();
        }
        dictFile.close();

        // Read String Line from the file
        FileReader fr = new FileReader("test_set_GBK.csv");
        BufferedReader br = new BufferedReader(fr);

        FileWriter fw = new FileWriter("result_11.9.a.csv", false); //replace if exit
        BufferedWriter bw = new BufferedWriter(fw);

        String text;
        Integer id = 0;
        while (br.ready()) {
            ++ id;
            text = br.readLine();
            Annotation document = new Annotation(text);
            StanfordCoreNLP corenlp = new StanfordCoreNLP("StanfordCoreNLP-chinese.properties");
            corenlp.annotate(document);
            // Output the Result
            parserOutput(document, bw, dict, text, id, comma);
        }
        fr.close();
        fw.close();
        } catch (Exception e) {
            System.out.println("File Error");
        }
    }

    public void parserOutput(Annotation document, BufferedWriter bw, HashMap dict, String ori, Integer id, String comma) {
        // these are all the sentences in this document
        // a CoreMap is essentially a Map that uses class objects as keys and has values with custom types
        List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);
        for (CoreMap sentence : sentences) {
            // this is the parse tree of the current sentence
            Tree tree = sentence.get(TreeCoreAnnotations.TreeAnnotation.class);

            //System.out.println("Tree: ");
            
            ArrayList<Tree> IPList = new ArrayList<Tree>();
            
            findIP(tree, IPList);
            
            ArrayList<Tree> NPList = new ArrayList<Tree>();
            ArrayList<Tree> VPList = new ArrayList<Tree>();
            String label = "";

            Tree tmpNN = null;
            Tree lastNP = null;
            Tree lastNN = null;
            String NNString = "";
            String VPString = "";
            String vpssub = "";
            ArrayList<String> VPStrings = new ArrayList<String>();
            ArrayList<String> NNStrings = new ArrayList<String>();
            ArrayList<String> scores = new ArrayList<String>();

            for(Tree IPNode : IPList){
                //Handle tree node with label written "IP"
                for(Tree child : IPNode.getChildrenAsList()){
                    label = (child.label()).toString();
                    if(label.equals("NP")){
                        NPList.add(child);
                    }else if(label.equals("VP")){
                        VPList.add(child);
                    }else{

                    }
                }
                //Handle with NP and VP
                if(!NPList.isEmpty()){
                    //Rule: last NP
                    lastNP = NPList.get(NPList.size()-1);
                    for(Tree itr : lastNP.getChildrenAsList()){
                        label = (((itr.label()).toString()).split("-"))[0];
                        if(label.equals("NN")){
                            lastNN = itr.getChild(0);
                        }
                    }
                    if(null == lastNN){
                        NNString = "NULL";
                    }else{
                        NNString = ((lastNN.toString()).split("-"))[0];
                    }
                }else{
                    NNString = "NULL";
                }

                if(NNStrings.contains(NNString)){
                    continue;
                }else{
                    //Rule: if "NN, NN", chose NN behind comma
                    String[] tmpnns = NNString.split(comma);
                    NNString = tmpnns[tmpnns.length-1];
                    NNStrings.add(NNString);
                }

                for(Tree itr : VPList){
                    VPString = "";
                    for(Tree leaf : itr.getLeaves()){
                        if(leaf == null)continue;
                        vpssub = (((leaf.label()).toString()).split("-"))[0];
                        if(vpssub.equals(comma)){
                            if(VPString == ""){
                                VPString = "NULL";
                                VPStrings.add(VPString);
                            }else{
                                if(!VPStrings.contains(VPString))
                                    VPStrings.add(VPString);
                            }
                        }else{
                            VPString = VPString + vpssub;
                        }
                    }

                    if(VPString == ""){
                        VPString = "NULL";
                        VPStrings.add(VPString);
                    }else{
                        if(!VPStrings.contains(VPString))
                            VPStrings.add(VPString);
                    }
                }

                Integer vpsssize = VPStrings.size();
                if(vpsssize == 0){
                    VPStrings.add("NULL");
                }
                while(NNStrings.size() < vpsssize){
                    NNStrings.add("NULL");
                }
            }

            try{
            
            bw.write(id.toString()+",");
            bw.flush();

            bw.write(ori+",");
            bw.flush();

            for(String nnstring : NNStrings){
                bw.write(nnstring+";");
                bw.flush();
            }

            bw.write(",");
            bw.flush();

            Object sentiment = null;
            ArrayList<String> sents = new ArrayList<String>();

            for(String vpstring : VPStrings){
                //give score
                if(vpstring.equals("NULL")){
                    scores.add("0");
                }else{
                    scores.add(Dafen(vpstring, dict));
                }

                bw.write(vpstring+";");
                bw.flush();
            }

            bw.write(",");
            bw.flush();

            for(String score : scores){
                bw.write(score+";");
                bw.flush();
            }

            bw.newLine();
            bw.flush();

            }catch(Exception e){

            }
            break;
        }
    }

    public String Dafen(String word, HashMap dict){
        String score = "0";
        Integer maxML = 0;
        Iterator<Map.Entry<String, String>> iterator = dict.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, String> entry = iterator.next();
            CharSequence akey = entry.getKey();            
            if(maxML < akey.length()){
                if(word.contains(akey)){
                    score = entry.getValue();
                    maxML = akey.length();
                    break;
                }
            }
        }
        return score;
    }

    public boolean findIP(Tree root, ArrayList<Tree> IPList){
        String label = "";
        boolean flag = false;
        for(Tree childNode : root.getChildrenAsList()){
            //System.out.println((childNode.label()).toString());
            label = (childNode.label()).toString();
            if(label.equals("IP")){
                flag = true;
                if(!findIP(childNode, IPList)){
                    IPList.add(childNode);
                }
            }else{
                if(findIP(childNode, IPList))
                    flag = true;
            }
        }
        return flag;
    }
}