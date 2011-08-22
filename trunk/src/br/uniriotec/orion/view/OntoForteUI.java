
package br.uniriotec.orion.view;

import br.uniriotec.orion.control.ForteInputGenerator;
import br.uniriotec.orion.model.forte.resources.Concept;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

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
        ForteInputGenerator gerador = new ForteInputGenerator(urlOntologia);
        List<Concept> conceitosRevisaveis = gerador.retrieveRevisableConcepts();
        
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
        
        /**************************************
         ** Criar os 4 arquivos para o FORTE **
         **************************************/
        try {
            gerador.generateDomainKnowledgeFile();
            gerador.generateTheoryRules();
            gerador.generateFundamentalTheory(conceitosParaRevisao);
            gerador.generateDataFile(conceitosParaRevisao);
        } catch (IOException ex) {
            Logger.getLogger(OntoForteUI.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        /*********************
         ** Invocar o FORTE **
         *********************/
        
     }

  
    

}
