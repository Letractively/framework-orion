
package br.uniriotec.orion.control ;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.hp.hpl.jena.ontology.DatatypeProperty;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.ObjectProperty;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.ontology.UnionClass;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.util.FileManager;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;

/**
 * <p>Classe responsevel por realizar o parse da ontologia. O parse é feito
 * através do framework Jena.</p>
 *
 * @author Felipe
 */
public class OntologyParser extends Object {

    
    //Variavel para armazenar a ontologia
    OntModel ontologia = null;
    
    @SuppressWarnings("unused")
	private OntologyParser(){}
    
    /**
     * Contrutor da classe. o parametro inputFileName deve ser o caminho da
     * ontologia a ser importada pelo Jena.
     * <p> O construtor cria um objeto para armazenar a ontologia no arquivo.
     * Caso o arquivo nao seja encontrado o construtor ira lancar uma excecao do
     * tipo IllegalArgumentException
     *
     * @param inputFileName
     * @throws IllegalArgumentException
     */
    public OntologyParser(String inputFileName) throws IllegalArgumentException {
        
        /**
         * Cria um objeto para armazenar a ontologia
         * O parametro passado desabilita o Reasoner (Habilitar um reasoner
         * sem necessidade gera resultados indesejados em pontos como ao buscar
         * a superclasse de uma classe, retornando mais de uma classe)
         */
        ontologia = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);

        //abre o arquivo validando sua existencia.
        InputStream arquivo = FileManager.get().open( inputFileName );
        if (arquivo == null) {
            throw new IllegalArgumentException( "File: " + inputFileName + " not found.");
        }

        // importa o arquivo RDF/XML para o objeto OntModel.
        ontologia.read(arquivo, "");
        // write it to standard out
        //model.write(System.out, "RDF/XML-ABBREV");

    }

    /**
     * Retorna um Set de todos os objetos OntClass da ontologia. O Set retornado
     *
     * @return Set<OntClass>
     */
    public Set<OntClass> listarClasses(){
        //Criar um iterador com as classes da ontologia
        ExtendedIterator<OntClass> classIterator = ontologia.listClasses();
        Set<OntClass> classes = new HashSet<OntClass>();
        //Objeto auxiliar para verificar se eh uma classe anonima
        OntClass aux = null;
        //para cada passo do iterador recupera-se a classe apontada e lista-se
        while (classIterator.hasNext()){
            aux = (OntClass) classIterator.next();
            if(aux.isAnon() == false){
                classes.add(aux);
            }
        }
        
        return classes;

    }
    
    /**
     * <p>retorna um Set de objetos OntClass, onde cada OntClass � uma subclasse
     * do objeto OntClass informado como parametro.</p>
     * 
     * @return Set<OntClass>
     */
    public Set<OntClass> listarSubclasses(OntClass classe){
    	Set<OntClass> subclasses = new HashSet<OntClass>();
    	Set<OntClass> conjClasses = listarClasses();
    	
    	for(OntClass c : conjClasses){
    		//Verifica se existe o axioma subClassOf
    		if(c.getSuperClass() == null){
    			continue;
    		}
    		
    		//Itera os axiomas subclassOf da classe
			ExtendedIterator<OntClass> it = c.listSuperClasses(true);
            OntClass aux = null;
            while(it.hasNext()){
                aux = it.next();
                
                //se tem LocalName definido, entao eh utilizado para referenciar superclasse
                if(aux.getLocalName() != null){ 
                    //Se TRUE entao a classe eh filha da classe informada como parametro.
                	if(aux.getLocalName().equals(classe.getLocalName())){
                    	subclasses.add(c);
                    }
                }
            }
    	}
    	
    	return subclasses;
    }

    /**
     * <p>Retorna um Set de objetos Individual, contendo todas as instancias
     * de todas as classes (objetos OntClass) ontologia.</p>
     *
     * @return Set<Individual>
     */
    public Set<Individual> listarInstancias(){
        //cria um Set de Individuals para ser retornado ao fim do metodo
        Set<Individual> instancias = new HashSet<Individual>();
        //Recupera todas as classes da ontologia
        Set<OntClass> classes = listarClasses();
        //Cria um iterador para o Set
        Iterator<OntClass> classesIterator = classes.iterator();

        //Para cada classe recuperar a lista de suas instancias
        while(classesIterator.hasNext()){
            OntClass classe = (OntClass)classesIterator.next();
            ExtendedIterator<? extends OntResource> individuals = classe.listInstances();
            //adicionar cada instancia ao conjunto a ser retornado
            while (individuals.hasNext()){
                Individual thisInstance = (Individual) individuals.next();
                instancias.add(thisInstance);
          }
        }
        //retorna conjunto de todas as instancias de todas as classes.
        return instancias;
    }

    
    /**
     * <p>Retorna um Set de objetos Individual, contendo todas as instancias
     * da classe (objeto OntClass) especificada no parametro.</p>
     * @return Set<Individual>
     */
    public Set<Individual> listarInstancias(OntClass classe){
        //cria um Set de Individuals para ser retornado ao fim do metodo
        Set<Individual> instancias = new HashSet<Individual>();
        //recupera iterador para todas as instancias da classe passada
        ExtendedIterator<? extends OntResource> individuals = classe.listInstances();

        //adicionar cada instancia ao conjunto a ser retornado
        while (individuals.hasNext()){
            Individual thisInstance = (Individual) individuals.next();
            instancias.add(thisInstance);
        }

        //retorna conjunto de todas as instancias da classe passada.
        return instancias;
    }


    /**
     * <p>Retorna um Set com todos os objetos DatatypeProperty presentes na
     * ontologia.</p>
     *
     * @return Set<DatatypeProperty>
     */
     public Set<DatatypeProperty> listarDatatypeProperties(){
         //Conjunto que sere retornado
         Set<DatatypeProperty> conjunto = new HashSet<DatatypeProperty>();
         //Criar um iterador com os Datatype Properties da ontologia
         ExtendedIterator<DatatypeProperty> datatypeProperties = ontologia.listDatatypeProperties();

         //adiciona ao conjunto de retorno todos os DatatypeProperties encontrados
         while (datatypeProperties.hasNext()) {
             DatatypeProperty dtProperty = (DatatypeProperty) datatypeProperties.next();
             conjunto.add(dtProperty);
         }

         return conjunto;
     }


     /**
      * <p>Retorna um Set com todos os objetos DatatypeProperty  utilizados
      * pela OntClass passada.</p>
      * 
      * @param ontClass
      * @return
      */
     public Set<DatatypeProperty> listarDatatypeProperties(OntClass ontClass){
         //Recuperar todos os DatatypeProperties
         Set<DatatypeProperty> conjuntoEntrada = listarDatatypeProperties();
         //Conjunto que sere retornado
         Set<DatatypeProperty> conjuntoSaida = new HashSet<DatatypeProperty>();

         //Loop para verificar cada Datatype da ontologia
         for(DatatypeProperty datatype : conjuntoEntrada){
            //Verifica se o Datatype é usado por multiplas classes
            if(datatype.getDomain().asClass().isUnionClass()){
                //é usado por multiplas classes
                //recuperar um iterador sobre o conjunto
                UnionClass uniao = datatype.getDomain().asClass().asUnionClass();
                ExtendedIterator<? extends OntClass> iterador = uniao.listOperands();
                //verifica se a classe passada é uma das classes do conjunto
                while(iterador.hasNext()){
                    OntClass classe = (OntClass) iterador.next();
                    //Se a classe passada faz parte do conjunto adicionar ao conjunto de retorno
                    if(classe.getLocalName().equals(ontClass.getLocalName())){
                        conjuntoSaida.add(datatype);
                    }
                }
            }else{
                //Não é usado por múltiplas classes
                //Se a classe passada faz parte do conjunto adicionar ao conjunto de retorno
                if(datatype.getDomain().getLocalName().equals(ontClass.getLocalName())){
                    conjuntoSaida.add(datatype);
                }
            }
         }
         return conjuntoSaida;

     }


     /**
     * Retorna um Set de objetos  todos os Object Relation da Ontologia
     */
     public Set<ObjectProperty> listarObjectProperties(){
         //Conjunto que sere retornado
         Set<ObjectProperty> conjunto = new HashSet<ObjectProperty>();
         //Criar um iterador com os objectRelations da ontologia
         ExtendedIterator<ObjectProperty> objProperties = ontologia.listObjectProperties();
        
         //adiciona ao conjunto todos os OntProperty encontrados
         while (objProperties.hasNext()) {
             ObjectProperty objProperty = (ObjectProperty) objProperties.next();
             conjunto.add(objProperty);
         }

         //retorna o conjunto final
         return conjunto;
     }
     
     /**
      * Metodo para recuperar um ObjectProperty com base em seu nome.
      * 
      * @param nome
      * @return
      */
     public ObjectProperty recuperarObjectProperty(String nome){
    	 Set<ObjectProperty> listaOP = listarObjectProperties();
    	 for(ObjectProperty op : listaOP){
    		 if(op.getLocalName().equalsIgnoreCase(nome)){
    			 return op;
    		 }
    	 }
    	 return null;
     }
     
    /**
     * Lista todos os statements da ontologia, no formato
     * [subject, predicate, object]
     */
    public void listarStatements(){
        // retorna um iterador para todos os statements da ontologia
        StmtIterator iter = ontologia.listStatements();
        while(iter.hasNext()){
            Statement stmt = iter.nextStatement();
            System.out.println(stmt);
        }
    }


    
    
}

