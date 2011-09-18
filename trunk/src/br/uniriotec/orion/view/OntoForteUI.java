
package br.uniriotec.orion.view;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import br.uniriotec.orion.control.ForteDataGenerator;
import br.uniriotec.orion.control.ForteFileGenerator;
import br.uniriotec.orion.model.forte.resources.Concept;

/**
 * <p>Classe responsável por realizar a interface com o usuário. Através desta classe é
 * possível dar entrada em uma ontologia no formato OWL/RDF e como output obtem-se um conjunto de
 * três arquivos que serão utilizados como input pelo FORTE.</p>
 * 
 * @author Felipe
 */
public class OntoForteUI {
    
     public OntoForteUI(){
        String urlOntologia = "src/input/orion/times_do_rio.owl";
        ForteDataGenerator dataGenerator = new ForteDataGenerator(urlOntologia);
        ForteFileGenerator fileGenerator = new ForteFileGenerator(urlOntologia);
        List<Concept> conceitosRevisaveis = dataGenerator.retrieveRevisableConcepts();
        
        System.out.println("\n=== Regras Revisaveis ===");
        for(Concept c : conceitosRevisaveis){
            System.out.println(c);
        }
        
        /******************************************
         ** Simular que os 4 primeiros conceitos ** 
         ** foram escolhidos para revisao        **
         ******************************************/
        List<Concept> conceitosParaRevisao = new ArrayList<Concept>();
        for(int i=0; i<4; i++){
            conceitosParaRevisao.add(conceitosRevisaveis.get(i));
        }
        System.out.println("\n=== Regras escolhidas para revisao ===");
        for(Concept c : conceitosParaRevisao){
            System.out.println(c);
        }
        
        //Divisao das listas necessaria para resolver BUG com gera��o de Intermediate Predicates
        List<Concept> listaDAT = new ArrayList<Concept>();
        listaDAT.addAll(conceitosParaRevisao);
        List<Concept> listaTHY = new ArrayList<Concept>();
        listaTHY.addAll(conceitosParaRevisao);
        List<Concept> listaFDT = new ArrayList<Concept>();
        listaFDT.addAll(conceitosParaRevisao);
        
        /**************************************
         ** Criar os 4 arquivos para o FORTE **
         **************************************/
        try {
        	fileGenerator.generateDomainKnowledgeFile();
        	fileGenerator.generateTheoryRules(listaTHY);
        	fileGenerator.generateFundamentalTheory(listaFDT);
        	fileGenerator.generateDataFile(listaDAT);
        } catch (IOException ex) {
            Logger.getLogger(OntoForteUI.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        /*********************
         ** Invocar o FORTE **
         *********************/
        
     }

  
    

}
