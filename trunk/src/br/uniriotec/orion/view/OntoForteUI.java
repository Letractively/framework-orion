
package br.uniriotec.orion.view;

import com.hp.hpl.jena.ontology.ObjectProperty;
import br.uniriotec.orion.control.ForteInputGenerator;
import java.util.Iterator;
import java.util.Set;
import br.uniriotec.orion.control.OntologyParser;

/**
 * <p>Classe responsável por realizar a interface com o usuário. Através desta classe é
 * possível dar entrada em uma ontologia no formal OWL/RDF e como output obtem-se um conjunto de
 * três arquivos que serão utilizados como input pelo FORTE.</p>
 * 
 * @author Felipe
 */
public class OntoForteUI {
    /*
     * Inicilmente será considerada a utilização de uma ontologia específica, sem
     * o input por um usuário. Será utilizada a ontologia padrão gerada pelo Protegé.
     */
     public OntoForteUI(){
        
         //Caminho da ontologia
        String urlOntologia = "C:/Users/Felipe/Desktop/OntoForte/Ontologia/instOntology_RDF.owl";

        /*
         * Carregar ontologia
         */
        ForteInputGenerator gerador = new ForteInputGenerator(urlOntologia);
        
        String top = gerador.generateTopLevelPredicates();


         System.out.println(top);
        



//        //Listar Instancias
//        //recupera a primeira classe da ontologia
//        OntClass classe = obj.listarClasses().iterator().next();
//        //Escreve a classe
//        System.out.println("Classe: "+classe.toString());
//        //lista todos os individuos da classe
//        escreverConjunto(obj.listarInstancias(classe));

     }

     
     /**
     * Escreve no Output todos os itens pertencentes ao conjunto passado.
     *
     * @param conjunto
     */
    private void escreverConjunto(Set conjunto){
        Iterator iterador = conjunto.iterator();
        while(iterador.hasNext()){
            System.out.println(iterador.next().toString());
        }
    }
    
    

}
