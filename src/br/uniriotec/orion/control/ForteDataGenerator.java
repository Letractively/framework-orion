
package br.uniriotec.orion.control;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import br.uniriotec.orion.model.forte.resources.Concept;
import br.uniriotec.orion.model.forte.resources.ConceptAttribute;
import br.uniriotec.orion.model.forte.resources.ConceptAxiom;
import br.uniriotec.orion.model.forte.resources.ConceptExample;
import br.uniriotec.orion.model.forte.resources.ConceptRestriction;
import br.uniriotec.orion.model.forte.resources.IExample;
import br.uniriotec.orion.model.forte.resources.ObjectAttribute;
import br.uniriotec.orion.model.forte.resources.Relationship;
import br.uniriotec.orion.model.forte.resources.RelationshipExample;

import com.hp.hpl.jena.ontology.CardinalityRestriction;
import com.hp.hpl.jena.ontology.DatatypeProperty;
import com.hp.hpl.jena.ontology.HasValueRestriction;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.MaxCardinalityRestriction;
import com.hp.hpl.jena.ontology.MinCardinalityRestriction;
import com.hp.hpl.jena.ontology.ObjectProperty;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.Restriction;
import com.hp.hpl.jena.ontology.SomeValuesFromRestriction;
import com.hp.hpl.jena.ontology.UnionClass;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;

/**
 * <p>Responsible for generate the inputs used by the Theory revision system.
 * Some data will still need user interaction, such as Concepts, wich will give
 * a lot of attribute that wont be necessary and the possible values of the
 * valid attributes
 * </p>
 *
 * @author Felipe
 */
public class ForteDataGenerator {
    public OntologyParser parser;

    public ForteDataGenerator(String inputFile){
        parser = new OntologyParser(inputFile);
    }

    /**
     * <p>Recupera todas as classes da ontologia e gera Conceitos com base nestas classes.
     * Todos os DataType Properties associados a classe sao inseridos no objeto Concept 
     * criado.</p>
     *
     * @return List<Concept>
     */
    public List<Concept> generateConcepts(){
        List<Concept> conceptsList = new ArrayList<Concept>();
        Set<OntClass> conjClasses = parser.listarClasses();
        Set<DatatypeProperty> conjDatatypes = null;

        for(OntClass ontClass : conjClasses){
            //Instanciar um Concept
            Concept tmpConcept = new Concept();
            //Adicionar o nome do conceito
            tmpConcept.setNome(lowerFirstChar(ontClass.getLocalName()));
            //recuperar todos os Datatype properties utilizados pela OntClass
            conjDatatypes = parser.listarDatatypeProperties(ontClass);
            Iterator<DatatypeProperty> iterator = conjDatatypes.iterator();

            
            /* Verificar se existem instancias declaradas para a classe na ontologia.
             * Se nao existir significa que a classe eh abstrata, e o metodo 
             * setAbstractConcept() deve ser invocado passando o valor TRUE.
             */
            if(parser.listarInstancias(ontClass).size() == 0){
            	tmpConcept.setAbstractConcept(true);
            }
            
            
            /* Para cada DatatypeProperty encontrado criar um objeto
             * ConceptAttribute e adiciona-lo a lista de atributos do Concept.
             */
            while(iterator.hasNext()){
                DatatypeProperty datatype = iterator.next();
                ConceptAttribute atrib = new ConceptAttribute();
                atrib.setNomeAtributo(lowerFirstChar(datatype.getLocalName()));
                atrib.setTipoRange(recuperarDomainsDatatype(datatype));
                //TODO interacao com o usuario para decidir se o atributo deve ser considerado
                tmpConcept.addAttribute(atrib);
            }
            
            
            /* Recupera a lista de classes que sao subclasses do conceito sendo gerado
             * e insere um ConceptAxiom para cada uma delas. Cada ConceptAxioma recebera
             * como nome "superClassOf" para que o conceito saiba reconhecer todas as 
             * suas subclasses.
             * 
             * OBS: isto eh necessario especialmente para quando o conceito eh abstrato,
             * jah que as regras que deverao ser geradas sobre o conceito devem indicar
             * suas possiveis instanciacoes.
             * 
             */
            Set<OntClass> conjSubclasses = parser.listarSubclasses(ontClass);
            if(conjSubclasses.size() != 0){
            	for(OntClass c: conjSubclasses){
            		ConceptAxiom axioma = new ConceptAxiom();
                    axioma.setNome("superClassOf");
                    axioma.setValor(lowerFirstChar(c.getLocalName()));
                    tmpConcept.addConceptAxiom(axioma);
            	}
            }
            
            /* Verificar todas as outras classes da ontologia e apurar se o conceito
             * que esta sendo criado eh disjunto a alguma destas outras classes. Sendo,
             * um axioma "disjointWith" deve ser criado. Esta geracao auxilia a evitar
             * que regras fiquem sem corpo, pois assume-se que caso nao haja nada
             * especifico para definir uma classe, ela ao menos eve ser disjunta de
             * outras classes.
             */
            for(OntClass c : conjClasses){
            	if(c.getDisjointWith() != null){
                    //iterador em cima de todas as classes disjuntas
                    Iterator<OntClass> it = c.listDisjointWith();
                    OntClass aux = null;
                    while(it.hasNext()){
                        aux = it.next();
                        //Se aux == ontClass entao o conceito (ontClass) e a 
                        //classe iterada (c) sao disjuntas
                        if(aux.getLocalName().equalsIgnoreCase(ontClass.getLocalName())){
                        	//Criacao do conceito negativo e verificacao para adicao
                            Concept conceitoNeg = criarConceitoNegativo(c);
                            if(isConceptInList(conceitoNeg, conceptsList) == false){
                            	conceptsList.add(conceitoNeg);	
                            }
                            
                           //adiciona a referencia de disjuncao
                            ConceptAxiom axioma = new ConceptAxiom();
                            axioma.setNome("disjointWith");
                            axioma.setValor(lowerFirstChar(conceitoNeg.getNome()));

                            tmpConcept.addConceptAxiom(axioma);
                        }
                    }
                }
            }
            
            
            
            /* Recupera todos os axiomas subClassOf da classe, identifica qual
             * faz referencia a classe pai e adiciona aos axiomas do conceito.
             * 
             * Ao recuperar a classe pai verifica-se se ela é abstrata. Sendo, os relacionamentos
             * sao inseridos no conceito.
             *
             * Recupera todos os restrictions indicados nos subclassOf restantes
             * e adiciona como restricoes no conceito atraves do metodo addRestriction
             * 
             * OBS: Restrictions sao Superclasses sem LocalName.
             */
            if(ontClass.getSuperClass() != null){
               Iterator<OntClass> it = ontClass.listSuperClasses(true);
               OntClass aux = null;
               while(it.hasNext()){
                   aux = it.next();
                   if(aux.getLocalName() != null){ //tem LocalName, referencia a classe pai
                       ConceptAxiom axioma = new ConceptAxiom();
                       axioma.setNome("subClassOf");
                       axioma.setValor(lowerFirstChar(aux.getLocalName()));
                       tmpConcept.addConceptAxiom(axioma);
                       
                       //Se a super classe for abstrata recupera todos os relacionamentos
                       List<ConceptRestriction> relacionamentosSup = recuperarRelacionamentosSup(aux);
                       for(ConceptRestriction conRest : relacionamentosSup){
                    	   tmpConcept.addConceptRestriction(conRest);
                       }
                   }else{ //eh um restriction
                       Restriction restriction = aux.asRestriction();
                       ConceptRestriction conRest = recuperarDadosRestriction(restriction);
                       tmpConcept.addConceptRestriction(conRest);
                       
                       //Verificar se existe mais de uma classe no range do OP e criar disjoints
                	   ObjectProperty op = parser.recuperarObjectProperty(conRest.getNomeProperty());
                	   if(op.getRange().asClass().isUnionClass()){
                		   UnionClass uniao = op.getRange().asClass().asUnionClass();
                           ExtendedIterator<? extends OntClass> iterador = uniao.listOperands();
                           while(iterador.hasNext()){
                               OntClass classe = (OntClass) iterador.next();
                               if(classe.getLocalName().equals(ontClass.getLocalName()) == false){
                            	   
                            	   //Criacao e adicao do conceito negativo a lista de conceitos
                            	   Concept conceitoNeg = criarConceitoNegativo(classe);
                                   if(isConceptInList(conceitoNeg, conceptsList) == false){
                                   	conceptsList.add(conceitoNeg);	
                                   }	
                                   
                                   //adiciona a referencia de disjuncao
                                   ConceptAxiom cAxioma = new ConceptAxiom();
                                   cAxioma.setNome("disjointWith");
                                   cAxioma.setValor(lowerFirstChar(conceitoNeg.getNome()));
                                   
                                   tmpConcept.addConceptAxiom(cAxioma);
                               }
                           }
                	   }
                   }
               }
            }

            /*
             * Recupera todos os axiomas DisjointWith do conceito e cria um conceito
             * auxiliar para permitir a negacao.
             *
             * Como o forte nao e capaz de revisar regras com negacao e necessario
             * criar um novo ceonceito que possui a negacao e inclui-lo no FDT.
             *
             * Cria-se entao um conceitoNegacao e um axioma subclassOf relacionando
             * a classe presente ao conceito negado, de forma que a geracao da regra
             * cria um predicado "regraNegacao(A)".
             */
            if(ontClass.getDisjointWith() != null){
                //iterador em cima de todas as classes disjuntas
                Iterator<OntClass> it = ontClass.listDisjointWith();
                OntClass aux = null;
                while(it.hasNext()){
                    aux = it.next();

                    //Criacao e adicao do conceito negativo a lista de conceitos
                    Concept conceitoNeg = criarConceitoNegativo(aux);
                    if(isConceptInList(conceitoNeg, conceptsList) == false){
                    	conceptsList.add(conceitoNeg);	
                    }
                    
                    //adiciona a referencia de disjuncao
                    ConceptAxiom axioma = new ConceptAxiom();
                    axioma.setNome("disjointWith");
                    axioma.setValor(lowerFirstChar(conceitoNeg.getNome()));

                    tmpConcept.addConceptAxiom(axioma);
                }
            }

            /*
             * Se a classe possuir o axioma EquivalentClass indicando sua
             * equivalencia a outra classe da ontologia entao ambas deverao
             * ser excluidas da revisao, sendo adicionadas ao FDT.
             *
             * Na geracao do conceito e inserido um axioma equivalentClass na lista
             * de axiomas com o valor igual ao nome do conceito ao qual se refere
             * a equivalencia. Desta forma sera possivel identificar posteriormente
             * os dois conceitos que deverao ser inseridos no FDT.
             */
            if(ontClass.getEquivalentClass() != null){
                //iterador em cima de todas as classes equivalentes
                Iterator<OntClass> it = ontClass.listEquivalentClasses();
                OntClass aux = null;
                while(it.hasNext()){
                    aux = it.next();
                    ConceptAxiom axioma = new ConceptAxiom();
                    axioma.setNome("equivalentClass");
                    axioma.setValor(lowerFirstChar(aux.getLocalName()));
                    tmpConcept.addConceptAxiom(axioma);
                }
            }

            conceptsList.add(tmpConcept);
        }
        
        
        return conceptsList;
    }
	

	/**
     * <p>Metodo para auxiliar a retornar somente os conceitos da ontologia que sao
     * revisaveis. O metodo recupera a lista gerada pelo metodo "generateConceps"
     * para recuperar todos os conceitos e entao aplica filtros para validar aqueles
     * que sao passiveis de revisao. Para um conceito ser revisavel ele:
     * <ol>
     * 	<li>Nao pode ser um conceito criado para negacao (iniciado por "nao")</li>
     *  <li>Nao pode ser um conceito abstrato</li>
     *  <li>Nao pode ser um conceito sem qualquer axioma</li>
     *  <li>Deve ter pelo menos um axioma indicando disjuncao de outros conceitos 
     *  	("disjointWith")</li>
     *  <li>Nao pode possuir axiomas "equivalentClass" ou ser citado por outros conceitos 
     *  	como um "equivalentClass"</li>
     * </ol>
     * </p>
     * <p>OBS: caso nao haja qualquer axioma de classe o conceito e tido como nao
     * revisavel, ja que nao possuir um axioma subClassOf indica ser filho de "Thing".
     * </p>
     * 
     * @return List<Concept>
     */
    public List<Concept> retrieveRevisableConcepts(){
        List<Concept> conceitos = generateConcepts();
        List<Concept> listaRetorno = new ArrayList<Concept>();
        List<String> classesEquivalentes = new ArrayList<String>();
        for(Concept c : conceitos){
            String prefixo = c.getNome().substring(0, 3);
            boolean conceitoRevisavel = false;
            //Se for um conceito de negacao, for abstrato ou nao possuir axiomas, nao eh revisavel
            if(prefixo.equals("nao") || c.isAbstractConcept() || c.getAxiomas() == null){
            	conceitoRevisavel = false;
            }else{
                for(ConceptAxiom ca : c.getAxiomas()){
                    //Se possuir um axioma "disjointWith" possivelmente eh revisavel
                	if(ca.getNome().equals("disjointWith")){
                    	conceitoRevisavel = true;
                    }
                }
                for(ConceptAxiom ca : c.getAxiomas()){
	                //Se possuir um axioma "equivalentClass", mesmo que tenha
	                //"disjointWith", nao eh revisavel
	        		if(ca.getNome().equals("equivalentClass")){
	                    conceitoRevisavel = false;
	                    classesEquivalentes.add(ca.getValor());
	                }
                }
            }
            
            //Se o conceito for revisavel, entao inseri-lo na lista de revisaveis
            if(conceitoRevisavel){
            	listaRetorno.add(c);
            }
        }
        
        //Retirar todos os conceitos citados por um axioma "equivalentClass"
        for(String cName : classesEquivalentes){
            for(Concept c : conceitos){
                if(c.getNome().equals(cName)){
                    if(listaRetorno.contains(c) == false){
                        listaRetorno.remove(c);
                    }
                }
            }
        }
        
        return listaRetorno;
    }
    
    /**
     * Este metodo retorna todos os conceitos que nao podem ser revisaveis. No caso
     * os que possuem o prefixo "nao" ou que sao subclasse de "Thing". Os conceitos
     * retornados por este metodo devem ser inseridos no arquivo FDT do FORTE.
     * 
     * Conceitos que possuem o axioma de classe EquivalentClass nao devem ser 
     * revisados, sendo diretamente encaminhados ao FDT.
     * 
     * @return 
     */
    public List<Concept> retrieveUnrevisableConcepts(){
        List<Concept> todosConceitos = generateConcepts();
        List<Concept> conceitosRevisaveis = retrieveRevisableConcepts();
        
        todosConceitos.removeAll(conceitosRevisaveis);
        return todosConceitos;
    }
    
    /**
     * Metodo para recuperar todos os conceitos abstratos da ontologia.
     * Um conceito eh tido como abstrato se o metodo isAbstractConcept() retorna
     * o valor TRUE. Conceitualmente, Conceitos abstratos sao aqueles que foram
     * gerados com base em Classes abstratas da ontologia, onde classes abstratas
     * sao aquelas que nao podem ser instanciadas na ontologia.
     * 
     * @return abstractConceptsList
     */
    public List<Concept> retrieveAbstractConcepts(){
    	List<Concept> conceitosNaoRevisaveis = retrieveUnrevisableConcepts();
    	List<Concept> abstractConceptsList = new ArrayList<Concept>();
    	for(Concept  c : conceitosNaoRevisaveis){
    		if(c.isAbstractConcept()){
    			abstractConceptsList.add(c);
    		}
    	}
    	return abstractConceptsList;
    }
    
    /**
     * Metodo para recuperar todos os conceitos negativos gerados pelo metodo "generateConcepts()"
     * para os axiomas de classe "disjointWith".
     * 
     * @return
     */
    public List<Concept> retrieveNegativeConcepts(){
    	List<Concept> conceitosNaoRevisaveis = retrieveUnrevisableConcepts();
    	List<Concept> negativeConcepts = new ArrayList<Concept>();
    	for(Concept  c : conceitosNaoRevisaveis){
    		if(c.getNome().substring(0,3).equals("nao") == true){
    			negativeConcepts.add(c);
    		}else{
    			//gerar regra "nao" para o conceito de rimeiro nivel
    			
    		}
    	}
    	return negativeConcepts;
    }
    
    /**
     * recupera todos os objetos ObjectProperty da ontologia e cria objetos
     * Relationship com base neles. √â considerado o caso de Range se referir
     * a mais de uma Classe (fazendo uso de um UnionOf)
     *
     * @return List<Relationship>
     */
    @SuppressWarnings("rawtypes")
	public List<Relationship> generateRelationships(){
        List<Relationship> relationshipList = new ArrayList<Relationship>();
        Set<ObjectProperty> conjObjProp = parser.listarObjectProperties();

        //Para cada ObjectProperty da Ontologia criar um objeto Relationship
        for(ObjectProperty objProp : conjObjProp){
            Relationship rel = new Relationship();
            rel.setNome(lowerFirstChar(objProp.getLocalName()));

            //Criar um Iterador em cima dos valores em RANGE
            //Caso haja mais de um, todos serao adicionados ao Relationship
            ExtendedIterator itRanges = objProp.listRange();
            List<String> termosRange = new ArrayList<String>();
            while(itRanges.hasNext()){
                OntClass rangeClasses = (OntClass)itRanges.next();
                //Se houver mais de um Range
                if(rangeClasses.isAnon()){
                    Iterator itRangeClasses = rangeClasses.asUnionClass().getOperands().iterator();
                    //Adiciona cada Range ao Relationship
                    while(itRangeClasses.hasNext()){
                        Resource range =  (Resource)itRangeClasses.next();
                        termosRange.add(lowerFirstChar(range.getLocalName()));
                    }
                //Havendo somente um Range
                }else{
                    termosRange.add(lowerFirstChar(rangeClasses.getLocalName()));
                }
                rel.setPrimeiroTermo(termosRange);
            }

            //Recuperar o Domain
            rel.setSegundoTermo(lowerFirstChar(objProp.getDomain().getLocalName()));

            //Adicionar Objeto a lista de retorno
            relationshipList.add(rel);
        }
        return relationshipList;
    }

    /**
     * <p>Metodo para geracao dos exemplos positivos da teoria a partir das
     * instancias fornecidas pela ontologia.
     * <br />
     * Exemplos podem ser gerados a partir de axiomas, relacionamentos (restrictions
     * do conceito) e datatypes (atributos do conceito).
     *
     * <br />Heuristicas Adotadas:
     * <li>
     * <ol>para cada instancia recuperar o nome de sua classe e seu LocalName </ol>
     * <ol>para cada instancia recuperar seus objectProperties e gerar exemplo com
     *  NomeOP(InstanciaLocalName,ResourceName)</ol>
     * </li>
     * </p>
     *
     * @return List<IExample>
     */
    public List<IExample> generatePositiveExamples(List<Concept> selectedConcepts){
        //TODO avisar na monografia que o sistema deve ser utilizado em ontologias concisas, 
    	//pois removera brechas para instancias ainda nao inseridas devido a especializacao
        List<IExample> conjExemplosPositivos = new ArrayList<IExample>();
        Set<Individual> conjIndividuals = parser.listarInstancias();
        Set<Individual> conjIndividualsSelected = new HashSet<Individual>();
        
        //verificar se existem instancias, nao existindo acaba o processamento
        if(conjIndividuals.isEmpty()){
            return null;
        }
        
        //Separar somente as instancias dos conceitos selecionados
        for(Individual i :conjIndividuals){
        	boolean isSelected = false;
        	for(Concept c : selectedConcepts){
        		if(i.getOntClass().getLocalName().equalsIgnoreCase(c.getNome())){
        			isSelected = true;
        		}
        	}
        	if(isSelected){
        		conjIndividualsSelected.add(i);
        	}
        }

        //////////////////////////////////////
        //          Heuristica 1            //
        //////////////////////////////////////
        for(Individual i : conjIndividualsSelected){
            ConceptExample exPosConcept = new ConceptExample();
            exPosConcept.setPredicado(lowerFirstChar(i.getOntClass().getLocalName()));
            exPosConcept.setPrimeiroTermo(lowerFirstChar(i.getLocalName()));
            conjExemplosPositivos.add(exPosConcept);
        }

//        //////////////////////////////////////
//        //          Heuristica 2            //
//        //////////////////////////////////////
//        Set<ObjectProperty> listaOP = parser.listarObjectProperties();
//        for(Individual i : conjIndividualsSelected){
//            StmtIterator iOP = i.listProperties();
//            while(iOP.hasNext()){
//                Statement stmt = iOP.next();
//                for(ObjectProperty o : listaOP){
//                    if(o.getURI().equals(stmt.getPredicate().toString())){
//                        String namespace = o.getNameSpace();
//                        RelationshipExample exPosRelationship = new RelationshipExample();
//                        exPosRelationship.setPredicado(o.getLocalName());
//                        exPosRelationship.setPrimeiroTermo(stmt.getSubject().toString().replaceFirst(namespace, ""));
//                        exPosRelationship.setSegundoTermo(stmt.getObject().toString().replaceFirst(namespace, ""));
//                        conjExemplosPositivos.add(exPosRelationship);
//                        continue;
//                    }
//                }
//            }
//        }

        return conjExemplosPositivos;
    }
    
    /**
     * A partir das Class identificar os DatatypeProperty e gerar exemplos positivos
     * (necessita de p√≥s interacao com usuario para preenchimento
     *
     * @return List<ObjectAttribute>
     */
    public List<ObjectAttribute> generateObjectAttributes(){
        Set<OntClass> conjClasses = parser.listarClasses();
        List<ObjectAttribute> conjAtributos = new ArrayList<ObjectAttribute>();
        for(OntClass classe : conjClasses){
            Set<DatatypeProperty> conjDatatypes = parser.listarDatatypeProperties(classe);
            ObjectAttribute atributo = null;
            for(DatatypeProperty dataProp : conjDatatypes){
                //TODO Adicionar a lista
                if(atributo == null){
                    atributo = new ObjectAttribute();
                    atributo.setRefConceito(classe.getLocalName());
                }
                atributo.addAtributo(dataProp.getLocalName());
            }
            if(atributo != null){
             conjAtributos.add(atributo);
            }
        }
        //TODO ainda nao tem os possiveis valores. Talves retirar possibilidades das instancias ou das descricoes dos  DatatypeProperties na OWL
        return conjAtributos;
    }

    /**
     * <p>Metodo para geracao dos exemplos negativos da teoria a partir das instancias da
     * ontologia com o auxilio de axiomas das classes.
     * <br />
     *
     * <br />Heuristicas Adotadas:
     * <li>
     * <ol>Recuperar classes que possuem o axioma <b>disjointWith</b>, procurar instancias das
     * classes disjuntas e utiliza-las para gerar exemplos das classes disjuntas. OBS: ao encontrar
     * uma disjuncao gerar exemplos negativos para as duas classes disjuntas.</ol>
     * <ol>Todas as instancias que nao forem da classe X devem servir para gerar exemplos
     * negativos da classe X.</ol>
     * </li>
     * </p>
     *
     * @return List<IExample>
     */
    public List<IExample> generateNegativeExamples(List<Concept> selectedConcepts){
        List<IExample> listaExemplosNegativos = new ArrayList<IExample>();
        Set<Individual> conjIndividuals = parser.listarInstancias();
        Set<OntClass> conjClasses = parser.listarClasses();

        //verificar se existem instancias, nao existindo acaba o processamento
        if(conjIndividuals.isEmpty()){
            return null;
        }

        //////////////////////////////////////
        //          Heuristica 1            //
        //////////////////////////////////////
//        for(OntClass classeOriginal : conjClasses){
//            if(classeOriginal.getDisjointWith() != null){
//                //iterador em cima de todas as classes disjuntas
//                Iterator<OntClass> itClassesDisjuntas = classeOriginal.listDisjointWith();
//                OntClass classeDisjunta = null;
//                //Enquanto houver classe disjunta
//                while(itClassesDisjuntas.hasNext()){
//                    classeDisjunta = itClassesDisjuntas.next();
//                    /**
//                     * passar classe original e classe disjunta para gerar exemplos negativos
//                     * para as duas. Adicionar todos os exemplos negativos ao conjunto de retorno.
//                     */
//                    listaExemplosNegativos.addAll(gerarExemplosDisjuntos(classeOriginal, classeDisjunta));
//                }
//            }
//        }

        //////////////////////////////////////
        //          Heuristica 2            //
        //////////////////////////////////////
        //Selecionar todas as OntClass correspondentes aos conceitos selecionados
        List<OntClass> ontClassesSelecionadas = new ArrayList<OntClass>();
        for(Concept concept : selectedConcepts){
        	for(OntClass ontClass : conjClasses){
        		if(ontClass.getLocalName().equalsIgnoreCase(concept.getNome())){
        			ontClassesSelecionadas.add(ontClass);
        		}
        	}
        }
        
        //Para cada OntClass recuperar todas os individuals dela e subtrair do conjunto
        //total de individuals. o conjunto final e usado para gerar os exemplos negativos
        //daquela OntClass
        for(OntClass ontClass : ontClassesSelecionadas){
            Set<Individual> individualsPos = parser.listarInstancias(ontClass);
            Set<Individual> individualsNeg = conjIndividuals;
            individualsNeg.removeAll(individualsPos);
            
            ConceptExample exNegConcept = null;
            
            for(Individual i :individualsNeg){
                exNegConcept = new ConceptExample();
                exNegConcept.setPredicado(lowerFirstChar(ontClass.getLocalName()));
                exNegConcept.setPrimeiroTermo(lowerFirstChar(i.getLocalName()));
                listaExemplosNegativos.add(exNegConcept);
            }
        }
        
        return listaExemplosNegativos;
    }

    /**
     * <p>Metodo responsavel por gerar os top_level_predicates no arquivo .DAT.
     * Um predicado e top_level quando N√O aparece no corpo de nenhuma outra regra.
     * Predicados que aparecem em corpo de regra sao intermediate.</p>
     */
    public List<Concept> generatePossibleTopLevelPredicates(){
    	List<Concept> conceitosRevisaveis = retrieveRevisableConcepts();
    	List<Concept> topLevel = new ArrayList<Concept>();
    	
    	for(Concept cTestado : conceitosRevisaveis){
    		boolean ehCorpoDeRegra = false;
    		for(Concept cAux : conceitosRevisaveis){
    			int qtdCharNome = cAux.getNome().length();
    			String corpoConceptTestado = cAux.toString().substring(qtdCharNome, cAux.toString().length());
    			if(corpoConceptTestado.contains(" "+cTestado.getNome()+"(")){
    				ehCorpoDeRegra = true;
    				break;
    			}
    		}
    		if(ehCorpoDeRegra == false){
    			topLevel.add(cTestado);
    		}
    	}
    	
    	return topLevel;
    }
    
    
    /**
     * <p>Metodo responsavel por gerar os intermediate_predicates no arquivo .DAT.
     * Um predicado eh intermediate quando for um predicado revisavel e aparecer
     * no corpo de uma regra que se encontra selecionada para revisao, ou seja,
     * estiver inclusa entre os top_level_predicates.</p>
     */
    public List<Concept> generateIntermediatePredicates(List<Concept> topLevelConcepts){
    	List<Concept> conceitosRevisaveis = new ArrayList<Concept>();
    	List<Concept> possibleIntermediateConcepts = new ArrayList<Concept>();
    	List<Concept> trueIntermediates = new ArrayList<Concept>();
    	
    	conceitosRevisaveis.addAll(retrieveRevisableConcepts());
    	possibleIntermediateConcepts.addAll(conceitosRevisaveis);
    	
    	//Primeiramente remover todos que sao top_level
    	possibleIntermediateConcepts.removeAll(topLevelConcepts);
    	//refinar deixando somente os conceitos que sao de fato predicados 
    	//intermediarios dos conceitos no top_level
    	for(Concept cInter : possibleIntermediateConcepts){
    		boolean isIntermediate = false;
    		for(Concept cTop : topLevelConcepts){
    			List<ConceptRestriction> restrictions = cTop.getRestrictions();
    			for(ConceptRestriction r : restrictions){
    				if(cInter.getNome().equalsIgnoreCase(r.getValorRestriction())){
    					isIntermediate = true;
    				}
    			}
    		}
    		if(isIntermediate){
    			trueIntermediates.add(cInter);
    		}
    	}
    	
    	return trueIntermediates;
    }
    
    /**
     * Este método gera os fatos utilizados pelo FORTE para provar os exemplos positivos e negar
     * os exemplos negativos. As seguntes abordagens sao utilizadas para gerar os fatos:
     * 
     * 	1) Os fatos com base nas instancias de conceitos que nao estao sofrendo revisao.
     * 	Estes fatos sao representados por objetos do tipo ConceptExample.
     * 
     *  2) Os fato com base nos relacionamentos. Considera-se que toda classe "nao abstrata"
     *  que possui uma restriction fazendo referencia a um relacionamento viabiliza a 
     *  criacao de fatos sobre este relacionamento, portanto com base nas instancias de 
     *  classes nao abstratas eh possivel gerar exemplos de relacionamentos.
     *  
     *  OBS: Caso existam relacionamentos que sao unicamente referenciados por classes
     *  abstratas se faz impossivel a geracao de exemplos, entretanto como a o relacionamento
     *  acabara nao sendo invocado por nenhuma regra sendo revisada a ausencia de exemplos
     *  nao devera impactar no processo de revisao e refinamento. 
     * 
     */
    public List<IExample> generateFacts(List<Concept> selectedConcepts){
        //Criar lista que sera retornada
    	List<IExample> conjFacts = new ArrayList<IExample>();
        //Recuperar todos os individuals (instancias)
    	Set<Individual> conjIndividuals = parser.listarInstancias();
    	//Conjunto com as instancias dos conceitos selecionados para revisao
        Set<Individual> conjIndividualsSelected = new HashSet<Individual>();
        
        /*
         * verificar se existem instancias na ontologia. Nao existindo se torna
         * impossivel gerar fatos sobre os conceitos. Uma vez que nao se tem
         * instancias e tambem impossivel gerar fatos sobre relacionamentos,
         * dado que estes sao retirados das instancias. Neste caso o metodo
         * eh rapidamente encerrado.
         */
        if(conjIndividuals.isEmpty()){
            return null;
        }
        
        
        /*
         * Gerar fatos com base nos relacionamentos. Passa-se por todo o conjunto de 
         * isntancias e procura-se em cada uma delas por relacionamentos. Uma vez encontrado
         * criase um objeto RelationshipExample que armazena o nome do relacionamento e os
         * dois termos referidos por ele.
         */
        for(Individual i :conjIndividuals){
        	StmtIterator it = i.listProperties();
        	while(it.hasNext()){
        		Statement p = it.next();
        		if((p.getObject().isResource()) && (p.getPredicate().getLocalName().equalsIgnoreCase("type") == false)){
        			RelationshipExample ex = new RelationshipExample();
		        		ex.setPredicado(lowerFirstChar(p.getPredicate().getLocalName()));
		        		ex.setPrimeiroTermo(lowerFirstChar(p.getSubject().getLocalName()));
		        		ex.setSegundoTermo(lowerFirstChar(p.getObject().asResource().getLocalName()));
		        	conjFacts.add(ex);
        		}
        	}
        }
        
        /*
         * Gerar fatos com base nos conceitos. Para isto separam-se os conceitos que
         * estao sendo revisados daqueles foram excluidos da revisao. Fatos sobre 
         * conceitos serao gerados somente com base em conceitos nao revisados.
         */
        for(Individual i :conjIndividuals){
        	boolean isSelected = false;
        	for(Concept c : selectedConcepts){
        		if(i.getOntClass().getLocalName().equalsIgnoreCase(c.getNome())){
        			isSelected = true;
        		}
        	}
        	if(isSelected){
        		conjIndividualsSelected.add(i);
        	}
        }
        
        //Remove do conjunto de individuals as instancias dos conceitos selecionados
        conjIndividuals.removeAll(conjIndividualsSelected);
        
        //Gera fatos com os individuals
        for(Individual i : conjIndividuals){
            ConceptExample fact = new ConceptExample();
            fact.setPredicado(lowerFirstChar(i.getOntClass().getLocalName()));
            fact.setPrimeiroTermo(lowerFirstChar(i.getLocalName()));
            conjFacts.add(fact);
        }
        
        return conjFacts;
    }
    
 

    //////////////////////////////////////////////////////////
    //                 Metodos Auxiliares                   //
    //////////////////////////////////////////////////////////
    

    /**
     * Metodo auxiliar para recuperar um ConceptRestriction a partir de um objeto
     * Restriction da API do JENA.
     *
     * @param restriction
     * @return mapRestriction
     */
    private ConceptRestriction recuperarDadosRestriction(Restriction restriction) {
        
       ConceptRestriction restricao = new ConceptRestriction();
       restricao.setNomeProperty(lowerFirstChar(restriction.getOnProperty().getLocalName()));
        
       if(restriction.isCardinalityRestriction()){
           CardinalityRestriction rest = restriction.asCardinalityRestriction();
           restricao.setTipoRestriction("cardinality");
           restricao.setValorRestriction(Integer.toString(rest.getCardinality()));
       
       }else if(restriction.isMaxCardinalityRestriction()){
           MaxCardinalityRestriction rest = restriction.asMaxCardinalityRestriction();
           restricao.setTipoRestriction("maxCardinality");
           restricao.setValorRestriction(Integer.toString(rest.getMaxCardinality()));
       
       }else if(restriction.isMinCardinalityRestriction()){
           MinCardinalityRestriction rest = restriction.asMinCardinalityRestriction();
           restricao.setTipoRestriction("minCardinality");
           restricao.setValorRestriction(Integer.toString(rest.getMinCardinality()));
       
       }else if(restriction.isSomeValuesFromRestriction()){
           SomeValuesFromRestriction rest = restriction.asSomeValuesFromRestriction();
           restricao.setTipoRestriction("someValuesFrom");
           restricao.setValorRestriction(lowerFirstChar(rest.getSomeValuesFrom().getLocalName()));
       
       }else if(restriction.isHasValueRestriction()){
           HasValueRestriction rest = restriction.asHasValueRestriction();
           restricao.setTipoRestriction("hasValue");
           restricao.setValorRestriction(lowerFirstChar(rest.getHasValue().asResource().getLocalName()));
       
       }else {
           restricao.setTipoRestriction("Tipo desconhecido");
           restricao.setValorRestriction("Valor desconhecido");
       }
       
       return restricao;

    }
    
    /**
     * Metodo para verificar se um conceito faz parte de uma lista de conceitos.
     * O metodo eh particularmente utilizado durante a geracao de conceitos pelo
     * metodo generateConcept(), pois eh possivel que o mesmo conceito negativo
     * seja gerado mais de uma vez, portanto antes que ele seja inserido na lista
     * de conceitos a serem retornados verifica-se se este ja se encontra incluso.
     * 
     * @param concept
     * @param conceptsList
     * @return
     */
    private boolean isConceptInList(Concept concept, List<Concept> conceptsList) {
		boolean isInList = false;
    	for(Concept c : conceptsList){
			if(c.getNome().equals(concept.getNome())){
				isInList = true;
			}
		}
		return isInList;
	}

    @SuppressWarnings("rawtypes")
	private List<String> recuperarDomainsDatatype(DatatypeProperty datatype) {
        //Criar um Iterador em cima dos valores em DOMAIN
        //Caso haja mais de um, todos serao adicionados ao Relationship
        ExtendedIterator itDomains = datatype.listDomain();
        List<String> termosDomain = new ArrayList<String>();
        while(itDomains.hasNext()){
            OntClass domainClasses = (OntClass)itDomains.next();
            //Se houver mais de um Range
            if(domainClasses.isAnon()){
                Iterator itDomainClasses = domainClasses.asUnionClass().getOperands().iterator();
                //Adiciona cada Range ao Relationship
                while(itDomainClasses.hasNext()){
                    Resource range =  (Resource)itDomainClasses.next();
                    termosDomain.add(lowerFirstChar(range.getLocalName()));
                }
            //Havendo somente um Range
            }else{
                termosDomain.add(lowerFirstChar(domainClasses.getLocalName()));
            }
        }

        return termosDomain;
    }

    /**
     * <p>Metodo auxiliar para ajudar na criacao de exemplos negativos atraves do uso
     * do axioma de classe DisjointWith. Dois par‚metros sao recebidos, a classeOriginal,
     * ou seja, aquela sobre a qual se deseja gerar exemplos negativos e a classeDisjunta,
     * ou seja, a classe sobre a qual serao recuperadas as inst‚ncias que serao utilizadas
     * para criar os exemplos negativos da classeOriginal.</p>
     * 
     * @param classeOriginal
     * @param classeDisjunta
     * @return List<IExample>
     */
    @SuppressWarnings("unused")
	private List<IExample> gerarExemplosDisjuntos(OntClass classeOriginal, OntClass classeDisjunta){
        List<IExample> listaExemplosNeg = new ArrayList<IExample>();
        Set<Individual> instanciasDisj = parser.listarInstancias(classeDisjunta);
        Set<Individual> instanciasOrig = parser.listarInstancias(classeOriginal);
        ConceptExample exNegConcept = null;

        //Gerar exemplos negativos da classe Original (com base nas instancias disjuntas)
        for(Individual i : instanciasDisj){
            exNegConcept = new ConceptExample();
            exNegConcept.setPredicado(lowerFirstChar(classeOriginal.getLocalName()));
            exNegConcept.setPrimeiroTermo(lowerFirstChar(i.getLocalName()));
            listaExemplosNeg.add(exNegConcept);
        }

        //Gerar exemplos negativos da classe Disjunta (com base nas instancias da classe original)
        for(Individual i : instanciasOrig){
            exNegConcept = new ConceptExample();
            exNegConcept.setPredicado(lowerFirstChar(classeDisjunta.getLocalName()));
            exNegConcept.setPrimeiroTermo(lowerFirstChar(i.getLocalName()));
            listaExemplosNeg.add(exNegConcept);
        }

        return listaExemplosNeg;
    }
    
    /**
     * Metodo para recuperar todos os relacionamentos de uma classe, caso esta seja abstrata.
     * O metodo opera de forma recursiva para recuperar os relacionamentos das super classes
     * caso elas tambem sejam abstratas.
     * 
     * @param aux
     * @return
     */
    private List<ConceptRestriction> recuperarRelacionamentosSup(OntClass ontClass) {
		
    	List<ConceptRestriction> relacionamentos = new ArrayList<ConceptRestriction>();
    	
		if(parser.listarInstancias(ontClass).size() == 0){
    		if(ontClass.getSuperClass() != null){
                Iterator<OntClass> it = ontClass.listSuperClasses(true);
                OntClass aux = null;
                while(it.hasNext()){
                    aux = it.next();
                    if(aux.getLocalName() == null){
                    	Restriction restriction = aux.asRestriction();
                        relacionamentos.add(recuperarDadosRestriction(restriction));
                    }else{
                    	relacionamentos.addAll(recuperarRelacionamentosSup(aux));
                    }
                }
             }
		}
		
		return relacionamentos;
	}
    
    /**
     * Metodo para criar e retornar um conceito negativo com base em um objeto OntClass
     * 
     * @param ontClass
     * @return
     */
    private Concept criarConceitoNegativo(OntClass ontClass) {
		Concept conceitoNeg = new Concept();
            ConceptAxiom axiomaNeg = new ConceptAxiom();
            axiomaNeg.setNome("subClassOf");
            axiomaNeg.setValor("\\+ "+lowerFirstChar(ontClass.getLocalName()));
        conceitoNeg.setNome("nao"+lowerFirstChar(ontClass.getLocalName()));
        conceitoNeg.addConceptAxiom(axiomaNeg);
        return conceitoNeg;
	}
    
    /**
     * Metodo para transformar o primeiro caracter de uma string em minusculo
     * 
     * @param s
     * @return lowerCaseFirstCharString
     */
    protected String lowerFirstChar(String s) {
		String firstChar = s.substring(0, 1).toLowerCase();
		String complemento = s.substring(1, s.length());
		return firstChar+complemento;
	}

}
