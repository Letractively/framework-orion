/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package br.uniriotec.orion.control;

import com.hp.hpl.jena.ontology.DatatypeProperty;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.UnionClass;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import java.util.Iterator;
import java.util.Set;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author Felipe
 */
public class OntologyParserTest {
    Set<DatatypeProperty> listaDatatypes;
    Set<OntClass> listaClasses;
    Iterator<DatatypeProperty> iteradorDatatypes;
    Iterator<OntClass> iteradorClasses;
    OntologyParser ontologia;

    public OntologyParserTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
        /*
        String urlOntologia = "src/instOntology_RDF.owl";
        ontologia = new OntologyParser(urlOntologia);
        listaClasses = ontologia.listarClasses();
        iteradorClasses = listaClasses.iterator();

        */
    }

    @After
    public void tearDown() {
    }

    
    /**
     * Test of listarDatatypeProperties method, of class OntologyParser.
     */
    @Test
    public void testListarDatatypeProperties_OntClass() {
//        while(iteradorClasses.hasNext()){
//            OntClass classe = iteradorClasses.next();
//            System.out.println("Classe: " + classe.getLocalName());
//            listaDatatypes = ontologia.listarDatatypeProperties(classe);
//            iteradorDatatypes = listaDatatypes.iterator();
//            while(iteradorDatatypes.hasNext()){
//                DatatypeProperty datatype = iteradorDatatypes.next();
//                System.out.println("-- Datatype: " + datatype.getLocalName());
//            }
//        }
    }

    

}