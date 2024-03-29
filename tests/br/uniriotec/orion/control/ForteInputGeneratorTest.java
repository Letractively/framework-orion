/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package br.uniriotec.orion.control;

import java.util.Iterator;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import br.uniriotec.orion.model.forte.resources.Concept;
import br.uniriotec.orion.model.forte.resources.ConceptAttribute;
import br.uniriotec.orion.model.forte.resources.ConceptAxiom;
import br.uniriotec.orion.model.forte.resources.ConceptRestriction;
import br.uniriotec.orion.model.forte.resources.ObjectAttribute;
import br.uniriotec.orion.model.forte.resources.Relationship;

/**
 *
 * @author Felipe
 */
public class ForteInputGeneratorTest {
    ForteDataGenerator gerador;

    public ForteInputGeneratorTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
//        String urlOntologia = "src/input/orion/instOntology_RDF.owl";
        String urlOntologia = "src/input/orion/times_do_rio.owl";
        gerador = new ForteDataGenerator(urlOntologia);
    }

    @After
    public void tearDown() {
    }


    /**
     * Testa o método generateConcept e verifica o valor dentro de cada atributo
     * de cada objeto da lista retornada
     */
//    @Test
    public void testEscreverManualmenteGenerateConcepts() {
        List<Concept> lista = gerador.generateConcepts();
        Iterator<Concept> it = lista.iterator();
        while(it.hasNext()){
            Concept conceito = it.next();
            System.out.println("\n== Conceito ==");
            System.out.println("-- Nome: " + conceito.getNome());
            
            if(conceito.getAtributos() != null){
                Iterator<ConceptAttribute> itAtt = conceito.getAtributos().iterator();
                while(itAtt.hasNext()){
                    ConceptAttribute att = itAtt.next();
                    System.out.println("--- DatatypeProperty -> "+att);
                }
            }

            if(conceito.getAxiomas() != null){
                Iterator<ConceptAxiom> itAx = conceito.getAxiomas().iterator();
                while(itAx.hasNext()){
                    ConceptAxiom ax = itAx.next();
                    System.out.println("--- Axiom -> "+ax);
                }
            }
            
            if(conceito.getRestrictions() != null){
                Iterator<ConceptRestriction> itRest = conceito.getRestrictions().iterator();
                while(itRest.hasNext()){
                    ConceptRestriction rest = itRest.next();
                    System.out.println("--- Restriction -> "+rest);
                }
            }

        }
    }


    /**
     * Executa o método generateConcepts e chama o método toString de cada conceito
     * para gerar a regra que define o ceonceito.
     */
//    @Test
    public void testGenerateConcepts(){
        System.out.println("\n\n========= Escrever Conceitos =========");
        List<Concept> lista = gerador.generateConcepts();
        Iterator<Concept> it = lista.iterator();
        Concept aux;

        while(it.hasNext()){
            aux = it.next();
            System.out.println(aux);
        }
    }

//    @Test
    public void testGenerateRelationships() {
        System.out.println("\n\n========= Escrever Relacionamentos =========");
        List<Relationship> lista = gerador.generateRelationships();
        Iterator<Relationship> it = lista.iterator();
        Relationship aux;

        while(it.hasNext()){
            aux = it.next();
            System.out.println(aux.getRelacionamentos());
        }
    }

    @Test
    public void testRetrieveUnrevisableConcepts(){
    	List<Concept> cjt = gerador.retrieveUnrevisableConcepts();
    	for(Concept c : cjt){
    		System.out.println(c);
    	}
    }
    

//    @Test
    public void testGenerateObjectAttributes(){
        System.out.println("\n\n========= Escrever Object Attributes =========");
        List<ObjectAttribute> lista = gerador.generateObjectAttributes();
        for(ObjectAttribute att : lista){
            System.out.println(att);
        }
    }


//    @Test
    public void testGenerateTopLevelPredicates() {
    }

    @Test
    public void testGenerateTheory() {
    	String corpoRegra = "eee.";
    	String[] arrayPredicadosCorpoRegra;
		corpoRegra = corpoRegra.substring(0, corpoRegra.length()-1);
		arrayPredicadosCorpoRegra = corpoRegra.split(", ");
		for(String s : arrayPredicadosCorpoRegra){
			System.out.println(s);
		}
    }

}