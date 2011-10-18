
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
import com.hp.hpl.jena.rdf.model.Resource;
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
     * Recupera todas as classes da ontologia e gera Conceitos com base nestas classes.
     * Todos os DataType Properties associados a classe são inseridos no objeto Concept criado
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

            /* Para cada DatatypeProperty encontrado criar um objeto
             * ConceptAttribute e adiciona-lo a lista de atributos do Concept.
             */
            while(iterator.hasNext()){
                DatatypeProperty datatype = iterator.next();
                ConceptAttribute atrib = new ConceptAttribute();
                atrib.setNomeAtributo(lowerFirstChar(datatype.getLocalName()));
                atrib.setTipoRange(recuperarDomainsDatatype(datatype));
                //interação com o usuário
                tmpConcept.addAttribute(atrib);
            }

            /* Recupera todos os axiomas subClassOf da classe, identifica qual
             * faz referencia a classe pai e adiciona aos axiomas do conceito.
             *
             * Recupera todos os restrictions indicados nos subclassOf restantes
             * e adiciona como restriçÃµes no conceito atraves do metodo addRestriction
             * OBS: Restrictions são Superclasses sem LocalName.
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
                   }else{ //e um restriction
                       Restriction restriction = aux.asRestriction();
                       tmpConcept.addConceptRestriction(recuperarDadosRestriction(restriction));
                   }
               }
            }

            /*
             * Recupera todos os axiomas DisjointWith do conceito e cria um conceito
             * auxiliar com para permitir a negação.
             *
             * Como o forte não e capaz de revisar regras com negação e necessário
             * criar um novo ceonceito que possui a negação e inclui-lo no FDT.
             *
             * Cria-se então um conceitoNegação e um axioma subclassOf relacionando
             * a classe presente ao conceito negado, de forma que a geração da regra
             * cria um predicado "regraNegacao(A)".
             */
            if(ontClass.getDisjointWith() != null){
                //iterador em cima de todas as classes disjuntas
                Iterator<OntClass> it = ontClass.listDisjointWith();
                OntClass aux = null;
                while(it.hasNext()){
                    aux = it.next();

                    //Criação do conceito negativo
                    Concept conceitoNeg = new Concept();
                    ConceptAxiom axiomaNeg = new ConceptAxiom();
                    axiomaNeg.setNome("subClassOf");
                    axiomaNeg.setValor("\\+ "+lowerFirstChar(aux.getLocalName()));
                    conceitoNeg.setNome("nao"+lowerFirstChar(aux.getLocalName()));
                    conceitoNeg.addConceptAxiom(axiomaNeg);

                    //Adiciona o conceito negativo a lista de conceitos
                    conceptsList.add(conceitoNeg);

                    //adiciona a referencia de disjunção
                    ConceptAxiom axioma = new ConceptAxiom();
                    axioma.setNome("disjointWith");
                    axioma.setValor(lowerFirstChar(conceitoNeg.getNome()));

                    tmpConcept.addConceptAxiom(axioma);
                }
            }

            /*
             * Se a classe possuir o axioma EquivalentClass indicando sua
             * equivalencia a outra classe da ontologia então ambas deverão
             * ser excluídas da revisão, sendo adicionadas ao FDT.
             *
             * Na geração do conceito e inserido um axioma equivalentClass na lista
             * de axiomas com o valor igual ao nome do conceito ao qual se refere
             * a equivalencia. Desta forma será possível identificar posteriormente
             * os dois conceitos que deverão ser inseridos no FDT.
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
     * Metodo para auxiliar a retornar somente os conceitos da ontologia que são
     * revisáveis. O metodo recupera a lista gerada pelo metodo "generateConceps"
     * e retira aqueles que foram criados como auxílio, possuindo o prefixo "nao"
     * ou que são subclasse de "Thing".
     * 
     * OBS: caso não haja qualquer axioma de classe o conceito e tido como não
     * revisável, já que não possuir um axioma subClassOf indica ser filho de
     * "Thing".
     * 
     * @return 
     */
    public List<Concept> retrieveRevisableConcepts(){
        List<Concept> conceitos = generateConcepts();
        List<Concept> listaRetorno = new ArrayList<Concept>();
        List<String> classesEquivalentes = new ArrayList<String>();
        for(Concept c : conceitos){
            String prefixo = c.getNome().substring(0, 3);
            if(prefixo.equals("nao") == false){
                boolean conceitoRevisavel = true;
                if(c.getAxiomas() == null){
                    conceitoRevisavel = false;
                }else{
                    for(ConceptAxiom ca : c.getAxiomas()){
                        //Se for subclasse de Thing não e revisavel
                        if(ca.getNome().equals("subClassOf") && ca.getValor().equals("thing")){
                            conceitoRevisavel = false;
                        }else if(ca.getNome().equals("equivalentClass")){
                            conceitoRevisavel = false;
                            classesEquivalentes.add(ca.getValor());
                        }
                    }
                }
                if(conceitoRevisavel){
                    listaRetorno.add(c);
                }
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
     * Este metodo retorna todos os conceitos que não podem ser revisáveis. No caso
     * os que possuem o prefixo "nao" ou que são subclasse de "Thing". Os conceitos
     * retornados por este metodo devem ser inseridos no arquivo FDT do FORTE.
     * 
     * Conceitos que possuem o axioma de classe EquivalentClass não devem ser 
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
     * Metodo para recuperar todos os conceitos de primeiro nível da ontologia.
     * Um conceito e dito de primeiro nível quando ele e subclasse direta de "Thing".
     * É importante identificar conceitos de primeiro nível pois estes não possuem
     * regras descritas na ontologia.
     * 
     * @return firstLevelConceptsList
     */
    public List<Concept> retrieveFirstLevelConcepts(){
    	List<Concept> conceitosNaoRevisaveis = retrieveUnrevisableConcepts();
    	List<Concept> firstLevelConceptsList = new ArrayList<Concept>();
    	for(Concept  c : conceitosNaoRevisaveis){
    		//TODO colocar na documentacao que os conceitos da ontologia não devem comecar por NAO
    		if(c.getNome().substring(0,3).equals("nao") == false){
    			firstLevelConceptsList.add(c);
    		}
    	}
    	return firstLevelConceptsList;
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
     * Relationship com base neles. Ã‰ considerado o caso de Range se referir
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
            //Caso haja mais de um, todos serão adicionados ao Relationship
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
     * <p>Metodo para geração dos exemplos positivos da teoria a partir das
     * instancias fornecidas pela ontologia.
     * <br />
     * Exemplos podem ser gerados a partir de axiomas, relacionamentos (restrictions
     * do conceito) e datatypes (atributos do conceito).
     *
     * <br />Heurísticas Adotadas:
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
    	//pois removera brechas para instancias ainda não inseridas devido a especialização
        List<IExample> conjExemplosPositivos = new ArrayList<IExample>();
        Set<Individual> conjIndividuals = parser.listarInstancias();
        Set<Individual> conjIndividualsSelected = new HashSet<Individual>();
        
        //verificar se existem instancias, não existindo acaba o processamento
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
        //          Heurística 1            //
        //////////////////////////////////////
        for(Individual i : conjIndividualsSelected){
            ConceptExample exPosConcept = new ConceptExample();
            exPosConcept.setPredicado(lowerFirstChar(i.getOntClass().getLocalName()));
            exPosConcept.setPrimeiroTermo(lowerFirstChar(i.getLocalName()));
            conjExemplosPositivos.add(exPosConcept);
        }

//        //////////////////////////////////////
//        //          Heurística 2            //
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
     * (necessita de pÃ³s interação com usuário para preenchimento
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
        //TODO ainda não tem os possíveis valores. Talves retirar possibilidades das instancias ou das descriçÃµes dos  DatatypeProperties na OWL
        return conjAtributos;
    }

    /**
     * <p>Metodo para geração dos exemplos negativos da teoria a partir das instancias da
     * ontologia com o auxílio de axiomas das classes.
     * <br />
     *
     * <br />Heurísticas Adotadas:
     * <li>
     * <ol>Recuperar classes que possuem o axioma <b>disjointWith</b>, procurar instancias das
     * classes disjuntas e utilizá-las para gerar exemplos das classes disjuntas. OBS: ao encontrar
     * uma disjunção gerar exemplos negativos para as duas classes disjuntas.</ol>
     * <ol>Todas as instancias que não forem da classe X devem servir para gerar exemplos
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

        //verificar se existem instancias, não existindo acaba o processamento
        if(conjIndividuals.isEmpty()){
            return null;
        }

        //////////////////////////////////////
        //          Heurística 1            //
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
        //          Heurística 2            //
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
     * <p>Metodo responsável por gerar os top_level_predicates no arquivo .DAT.
     * Um predicado e top_level quando NÃO aparece no corpo de nenhuma outra regra.
     * Predicados que aparecem em corpo de regra são intermediate.</p>
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
     * <p>Metodo responsável por gerar os intermediate_predicates no arquivo .DAT.
     * Um predicado e intermediate quando aparece no corpo de outra regra.</p>
     */
    public List<Concept> generateIntermediatePredicates(List<Concept> topLevelConcepts){
    	List<Concept> conceitosRevisaveis = new ArrayList<Concept>();
    	List<Concept> possibleIntermediateConcepts = new ArrayList<Concept>();
    	List<Concept> trueIntermediates = new ArrayList<Concept>();
    	
    	conceitosRevisaveis.addAll(retrieveRevisableConcepts());
    	possibleIntermediateConcepts.addAll(conceitosRevisaveis);
    	
    	//Primeiramente remover todos que não são top_level
    	possibleIntermediateConcepts.removeAll(topLevelConcepts);
    	//refinar deixando somente os conceitos que são de fato predicados 
    	//intermediarios dos conceitos no top_level
    	for(Concept cInter : possibleIntermediateConcepts){
    		boolean isIntermediate = false;
    		for(Concept cTop : topLevelConcepts){
    			List<ConceptAxiom> axiomas = cTop.getAxiomas();
    			for(ConceptAxiom a : axiomas){
    				if(cInter.getNome().equalsIgnoreCase(a.getValor())){
    					isIntermediate = true;
    				}
    			}
    		}
    		if(isIntermediate){
    			trueIntermediates.add(cInter);
    		}
    	}
    	
    	//TODO verificar se relacionamentos podem ser intermediates  ou se todos devem ir para o FDT
    	return trueIntermediates;
    }
    
    /**
     * Recuperar todas as instancias dos conceitos que não estão sendo revisados para
     * formar o conjunto de fatos. As instancias dos relacionamentos tambem devem compor
     * o conjunto de fatos da teoria.
     */
    public List<IExample> generateFacts(List<Concept> selectedConcepts){
        //Criar lista que será retornada
    	List<IExample> conjFacts = new ArrayList<IExample>();
        //Recuperar todos os individuals (instancias)
    	Set<Individual> conjIndividuals = parser.listarInstancias();
    	//Conjunto com as instancias dos conceitos selecionados para revisao
        Set<Individual> conjIndividualsSelected = new HashSet<Individual>();
        
        //verificar se existem instancias, não existindo acaba o processamento
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
    
 

    /*********************************************************
     **                 Metodos Auxiliares                  **
     *********************************************************/

    /**
     * Metodo auxiliar para recuperar um ConceptRestriction a partir de um objeto
     * Restriction da API do JENA.
     *
     * @param restriction
     * @return mapRestriction
     */
    private ConceptRestriction recuperarDadosRestriction(Restriction restriction) {
        
       ConceptRestriction restricao = new ConceptRestriction();
       restricao.setNomeProperty(restriction.getOnProperty().getLocalName());
        
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

    @SuppressWarnings("rawtypes")
	private List<String> recuperarDomainsDatatype(DatatypeProperty datatype) {
        //Criar um Iterador em cima dos valores em DOMAIN
        //Caso haja mais de um, todos serão adicionados ao Relationship
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
     * <p>Metodo auxiliar para ajudar na criação de exemplos negativos atraves do uso
     * do axioma de classe DisjointWith. Dois parâmetros são recebidos, a classeOriginal,
     * ou seja, aquela sobre a qual se deseja gerar exemplos negativos e a classeDisjunta,
     * ou seja, a classe sobre a qual serão recuperadas as instâncias que serão utilizadas
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
