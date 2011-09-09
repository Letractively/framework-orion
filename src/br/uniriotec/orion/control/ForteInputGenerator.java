
package br.uniriotec.orion.control;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
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
public class ForteInputGenerator {
    public OntologyParser parser;

    public ForteInputGenerator(String inputFile){
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
             * e adiciona como restriçÃµes no conceito atraves do método addRestriction
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
                   }else{ //é um restriction
                       Restriction restriction = aux.asRestriction();
                       tmpConcept.addConceptRestriction(recuperarDadosRestriction(restriction));
                   }
               }
            }

            /*
             * Recupera todos os axiomas DisjointWith do conceito e cria um conceito
             * auxiliar com para permitir a negação.
             *
             * Como o forte não é capaz de revisar regras com negação é necessário
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
                    axiomaNeg.setValor("not "+lowerFirstChar(aux.getLocalName()));
                    conceitoNeg.setNome("nao"+lowerFirstChar(aux.getLocalName()));
                    conceitoNeg.addConceptAxiom(axiomaNeg);

                    //Adiciona o conceito negativo Ã  lista de conceitos
                    conceptsList.add(conceitoNeg);

                    //adiciona a referencia de disjunção
                    ConceptAxiom axioma = new ConceptAxiom();
                    axioma.setNome("disjointWith");
                    axioma.setValor(conceitoNeg.getNome());

                    tmpConcept.addConceptAxiom(axioma);
                }
            }

            /*
             * Se a classe possuir o axioma EquivalentClass indicando sua
             * equivalencia a outra classe da ontologia então ambas deverão
             * ser excluídas da revisão, sendo adicionadas ao FDT.
             *
             * Na geração do conceito é inserido um axioma equivalentClass na lista
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
                    axioma.setValor(aux.getLocalName());
                    tmpConcept.addConceptAxiom(axioma);
                }
            }

            conceptsList.add(tmpConcept);
        }

        return conceptsList;
    }
    
    
	/**
     * Método para auxiliar a retornar somente os conceitos da ontologia que são
     * revisáveis. O método recupera a lista gerada pelo método "generateConceps"
     * e retira aqueles que foram criados como auxílio, possuindo o prefixo "nao"
     * ou que são subclasse de "Thing".
     * 
     * OBS: caso não haja qualquer axioma de classe o conceito é tido como não
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
                        //Se for subclasse de Thing não é revisavel
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
     * Este método retorna todos os conceitos que não podem ser revisáveis. No caso
     * os que possuem o prefixo "nao" ou que são subclasse de "Thing". Os conceitos
     * retornados por este método devem ser inseridos no arquivo FDT do FORTE.
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
            rel.setNome(objProp.getLocalName());

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
                        termosRange.add(range.getLocalName());
                    }
                //Havendo somente um Range
                }else{
                    termosRange.add(rangeClasses.getLocalName());
                }
                rel.setPrimeiroTermo(termosRange);
            }

            //Recuperar o Domain
            rel.setSegundoTermo(objProp.getDomain().getLocalName());

            //Adicionar Objeto Ã  lista de retorno
            relationshipList.add(rel);
        }
        return relationshipList;
    }

    /**
     * <p>Método para geração dos exemplos positivos da teoria a partir das
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
    public List<IExample> generatePositiveExamples(){
        //TODO avisar na monografia que o sistema deve ser utilizado em ontologias concisas, pois removera brechas para instancias ainda não inseridas devido a especialização
        List<IExample> conjExemplosPositivos = new ArrayList<IExample>();
        Set<Individual> conjIndividuals = parser.listarInstancias();
        
        //verificar se existem instancias, não existindo acaba o processamento
        if(conjIndividuals.isEmpty()){
            return null;
        }

        //////////////////////////////////////
        //          Heurística 1            //
        //////////////////////////////////////
        for(Individual i : conjIndividuals){
            ConceptExample exPosConcept = new ConceptExample();
            exPosConcept.setPredicado(i.getOntClass().getLocalName());
            exPosConcept.setPrimeiroTermo(i.getLocalName());
            conjExemplosPositivos.add(exPosConcept);
        }

        //////////////////////////////////////
        //          Heurística 2            //
        //////////////////////////////////////
        Set<ObjectProperty> listaOP = parser.listarObjectProperties();
        for(Individual i : conjIndividuals){
            StmtIterator iOP = i.listProperties();
            while(iOP.hasNext()){
                Statement stmt = iOP.next();
                for(ObjectProperty o : listaOP){
                    if(o.getURI().equals(stmt.getPredicate().toString())){
                        String namespace = o.getNameSpace();
                        RelationshipExample exPosRelationship = new RelationshipExample();
                        exPosRelationship.setPredicado(o.getLocalName());
                        exPosRelationship.setPrimeiroTermo(stmt.getSubject().toString().replaceFirst(namespace, ""));
                        exPosRelationship.setSegundoTermo(stmt.getObject().toString().replaceFirst(namespace, ""));
                        conjExemplosPositivos.add(exPosRelationship);
                        continue;
                    }
                }
            }
        }

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
                //TODO Adicionar Ã  lista
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
     * <p>Método para geração dos exemplos negativos da teoria a partir das instancias da
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
    public List<IExample> generateNegativeExamples(){
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
        for(OntClass classeOriginal : conjClasses){
            Set<Individual> individualsPos = parser.listarInstancias(classeOriginal);
            Set<Individual> individualsNeg = conjIndividuals;
            individualsNeg.removeAll(individualsPos);
            
            ConceptExample exNegConcept = null;
            
            for(Individual i :individualsNeg){
                exNegConcept = new ConceptExample();
                exNegConcept.setPredicado(classeOriginal.getLocalName());
                exNegConcept.setPrimeiroTermo(i.getLocalName());
                listaExemplosNegativos.add(exNegConcept);
            }
        }
        
        return listaExemplosNegativos;
    }

    /**
     * <p>Método responsável por gerar os top_level_predicates no arquivo .DAT.
     * Um predicado é top_level quando NÃO aparece no corpo de nenhuma outra regra.
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
     * <p>Método responsável por gerar os intermediate_predicates no arquivo .DAT.
     * Um predicado é intermediate quando aparece no corpo de outra regra.</p>
     */
    public List<Concept> generateIntermediatePredicates(){
    	List<Concept> conceitosRevisaveis = retrieveRevisableConcepts();
    	List<Concept> topLevel = generatePossibleTopLevelPredicates();
    	List<Concept> intermediate = null;
    	
    	intermediate = conceitosRevisaveis;
    	intermediate.removeAll(topLevel);
    	
    	return intermediate;
    }
    /**
     * Recuperar todos os conceitos filhos de Thing e gerar fatos com base em 
     * suas instancias.
     * Recuperar todos os exemplos de Object Properties
     */
    public List<IExample> generateFacts(){
    	return new ArrayList<IExample>();
    }
    
    /**
     * Gera o arquivo THY, que comporta as regras que compõe a teoria. As regras do 
     * arquivo THY são todas as regras que foram selecionadas pelo usuário para revisão
     * somadas aos predicados identificados como "intermediate_predicates", ou seja, que
     * são utilizados no corpo das regras selecionadas para revisão.
     * 
     * OBS: Predicados no corpo de regras que estão definidos no arquivo FDT receberão
     *  o prefixo "fdt:".
     * 
     * @param rulesForRevision
     * @throws IOException 
     */
    public void generateTheoryRules(List<Concept> rulesForRevision) throws IOException{
    	List<Concept> conceitosRevisaveis = rulesForRevision;
    	conceitosRevisaveis.addAll(generateIntermediatePredicates());
    	
    	//Criar lista com todos os predicados que não requisitam o prefixo "fdt:"
    	List<String> cabecaPredicadosTHY = new ArrayList<String>();
    	for(Concept c : conceitosRevisaveis){
    		cabecaPredicadosTHY.add(c.getNome()+"(A)");
    	}
    	
    	//Preparar arquivo THY para escrita
    	BufferedWriter writter = new BufferedWriter(new FileWriter("src/input/forte/TheoryRules.thy"));
    	
        for(Concept c : conceitosRevisaveis){
        	/* verificar se os predicados no corpo das regras fazem parte do "rulesForRevision"
        	 * ou do intermediatePredicates, senão, inserir "fdt:", pois o predicado se encontra
        	 * definido no arquivo FDT.
        	 */
        	int posSinalImplicacao = c.toString().indexOf(":-");
        	String cabecaRegra = c.toString().substring(0, posSinalImplicacao-1);
        	String corpoRegra = c.toString().substring(posSinalImplicacao+3, c.toString().length());
        	String corpoRegraPrefixado = "";
        	
        	String[] arrayPredicadosCorpoRegra = corpoRegra.split(", ");
        	
        	for(String s : arrayPredicadosCorpoRegra){
        		boolean isCabeca = false;
        		for(String cabecaPred : cabecaPredicadosTHY){
        			if(cabecaPred.equals(s)){
        				isCabeca = true;
            		}	
        		}
        		if(isCabeca){
        			corpoRegraPrefixado += s+"; ";
        		}else{
        			corpoRegraPrefixado += "fdt:"+s+", ";
        		}	
        	}
        	
        	//trocar ultimo ", " por "."
        	corpoRegraPrefixado = corpoRegraPrefixado.substring(0, corpoRegraPrefixado.length()-2);
        	
        	//Escrever a regra no arquivo
            writter.append(cabecaRegra+" :- "+corpoRegraPrefixado+"\n");
        }
        writter.flush();
    }
    
    /** 
     * Gera o arquivo FDT, que informa ao FORTE o conhecimento fundamental sobre a teoria
     * que será revisada. O arquivo FDT deverá comportar conceitos tidos como não
     * revisáveis e todas as regras originadas de Relacionamentos (Relationship).
     * Algumas regras revisáveis podem ser excluidas da revisão pelo usuário, neste caso
     * devem ser adicionadas ao FDT.
     * 
     * @throws IOException 
     */
    public void generateFundamentalTheory(List<Concept> rulesForRevision) throws IOException{
        List<Concept> conceitosNaoRevisaveis = retrieveUnrevisableConcepts();
        List<Concept> conceitosRevisaveis = retrieveRevisableConcepts();
        List<Relationship> relacionamentos = generateRelationships();
        
        //Lista de conceitos revisáveis que o usuário optou por não revisar
        List<Concept> conceitosRevisaveisExcluidos = conceitosRevisaveis;
        conceitosRevisaveisExcluidos.removeAll(rulesForRevision);
        
        String moduloFDT = ":- module(fdt, [";
        String conceitosFDT = "\n\n";
        String relacionamentosFDT = "\n\n";
        
        //Escrever conceitos Não revisáveis
        for(Concept c : conceitosNaoRevisaveis){
            conceitosFDT += c.getNome() + "(X) :- example(" + c.getNome() + "(X)" + ").\n";
            moduloFDT += c.getNome() + "/1, ";
        }
        
        //Escrever Conceitos revisaveis excluidos da revisão
        for(Concept c : conceitosRevisaveisExcluidos){
            conceitosFDT += c.toString()+"\n";
            moduloFDT += c.getNome() + "/1, ";
        }
        
        //Escrever relacionamentos
        for(Relationship r : relacionamentos){
            relacionamentosFDT += r.toString()+".\n";
            moduloFDT += r.getNome() + "/2, ";
        }
        
        moduloFDT = moduloFDT.substring(0, moduloFDT.length()-2) + "]).";
        
        BufferedWriter writter = new BufferedWriter(new FileWriter("src/input/forte/FundamentalTheory.fdt"));
        writter.append(moduloFDT);
        writter.append(conceitosFDT);
        writter.append(relacionamentosFDT);
        writter.flush();
    }
    
    /**
     * Gera o arquivo DAT, que informa ao FORTE os seguintes dados básicos:
     *  - top_level_predicates;
     *  - intermediate_predicates;
     *  - strata (deixar em branco);
     *  - shielded (contem todos os predicados da teoria que não serão revisados);
     *  - object_attributes;
     *  - object_relations (definição dos fatos);
     *  - language_bias (usando padrão);
     *  - example (positivo, negativo, Objects, fatos
     * 
     * @throws IOException 
     */
    public void generateDataFile(List<Concept> rulesForRevision) throws IOException{
        List<Concept> topLevelConcepts = rulesForRevision;
    	List<Concept> intermediateConcepts = generateIntermediatePredicates();
    	List<IExample> fatos = generateFacts();
    	
    	List<String> topLevelPredicates = new ArrayList<String>();
    	List<String> intermediatePredicates = new ArrayList<String>();
    	Set<String> variaveis = new HashSet<String>();
    	
    	//Recuperar predicados de top_level e intermediate.
    	//recupera o nome do predicado e a sua superclasse, gera-se entao uma variavel
    	for(Concept c: topLevelConcepts){
    		String variavel = "var"+c.getAxiomas().get(0).getValor();
    		topLevelPredicates.add(c.getNome()+ "(" + variavel + ")");
    		variaveis.add(variavel);
    	}
    	for(Concept c: intermediateConcepts){
    		String variavel = "var"+c.getAxiomas().get(0).getValor();
    		intermediatePredicates.add(c.getNome()+ "(" + variavel + ")");
    		variaveis.add(variavel);
    	}
    	
    	/*======================================+
    	 * 		Preparar Texto para escrita		*
    	 *======================================*/
    	
    	//Preparar Top Level Predicate
    	String top_level = "top_level_predicates([";
    	for(String s : topLevelPredicates){
    		top_level += s+", ";
        }
    	top_level = top_level.substring(0, top_level.length()-2) + "]).";
    	
    	//Preparar Intermediate Predicate
    	String intermediate = "intermediate_predicates([";
    	for(String s : intermediatePredicates){
    		intermediate += s+", ";
        }
    	intermediate = intermediate.substring(0, intermediate.length()-2) + "]).";

    	//Preparar Strata
    	String strata = "strata([]).";
    	
    	//Preparar Shielded
    	String shielded = "shielded([";
    	List<Concept> conjuntoRegrasNaoRevisadas = retrieveRevisableConcepts();
    	conjuntoRegrasNaoRevisadas.removeAll(rulesForRevision);
    	for(Concept c : conjuntoRegrasNaoRevisadas){
    		shielded += c.getNome()+"(_), ";
    	}
    	shielded = shielded.substring(0, shielded.length()-2) + "]).";
    	
    	//Preparar Object Relations
    	//TODO conferir se a composição do elemento está certa (é pra entrar OP mesmo?)
    	String objectRel = "object_relations([";
    	List<Relationship> relacionamentos = generateRelationships();
    	for(Relationship r : relacionamentos){
    		String v1 = "var" + lowerFirstChar(r.getPrimeiroTermo().get(0));
    		String v2 = "var" + lowerFirstChar(r.getSegundoTermo());
    		objectRel += r.getNome()+ "(" + v1 + "," + v2 + "), ";
    		variaveis.add(v1);
    		variaveis.add(v2);
    	}
    	objectRel = objectRel.substring(0, objectRel.length()-2) + "]).";
    	
    	//Preparar Object Attribute
    	String objectAttr = "object_attributes([";
    	for(String s : variaveis){
    		objectAttr += s+"([]), ";
    	}
    	objectAttr = objectAttr.substring(0, objectAttr.length()-2) + "]).";
    	
    	
    	//Realizar a escrita em arquivo
    	BufferedWriter writter = new BufferedWriter(new FileWriter("src/input/forte/KnowledgeData.dat"));
        writter.append(top_level+"\n\n");
        writter.append(intermediate+"\n\n");
        writter.append(strata+"\n\n");
        writter.append(shielded+"\n\n");
        writter.append(objectAttr+"\n\n");
        writter.append(objectRel+"\n\n");
        writter.append("language_bias([depth_limit(5), use_attr, use_relations, " +
        		"use_theory, use_built_in, relation_tuning(highly_relational)]).");
        writter.flush();
    }
    
    /**
     * Gera o arquivo DOM, que informa ao FORTE os arquivos que devem ser incluídos
     * na execução do sistema para descrever o domínio da teoria.
     * 
     * @throws IOException 
     */
    public void generateDomainKnowledgeFile() throws IOException{
        BufferedWriter writter = new BufferedWriter(new FileWriter("src/input/forte/TheoryDomain.dom"));
        writter.append(":- compile('FundamentalTheory.fdt').\n");
        writter.append(":- compile('default.rv').\n");
        writter.append(":- compile('default.et').\n");
        writter.append(":- compile('default.tt').\n");
        writter.flush();
    }



    /*********************************************************
     **                 Métodos Auxiliares                  **
     *********************************************************/

    /**
     * Método auxiliar para recuperar um ConceptRestriction a partir de um objeto
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
           restricao.setValorRestriction(rest.getSomeValuesFrom().getLocalName());
       }else if(restriction.isHasValueRestriction()){
           HasValueRestriction rest = restriction.asHasValueRestriction();
           restricao.setTipoRestriction("hasValue");
           restricao.setValorRestriction(rest.getHasValue().asResource().getLocalName());
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
                    termosDomain.add(range.getLocalName());
                }
            //Havendo somente um Range
            }else{
                termosDomain.add(domainClasses.getLocalName());
            }
        }

        return termosDomain;
    }

    /**
     * <p>Método auxiliar para ajudar na criação de exemplos negativos através do uso
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
            exNegConcept.setPredicado(classeOriginal.getLocalName());
            exNegConcept.setPrimeiroTermo(i.getLocalName());
            listaExemplosNeg.add(exNegConcept);
        }

        //Gerar exemplos negativos da classe Disjunta (com base nas instancias da classe original)
        for(Individual i : instanciasOrig){
            exNegConcept = new ConceptExample();
            exNegConcept.setPredicado(classeDisjunta.getLocalName());
            exNegConcept.setPrimeiroTermo(i.getLocalName());
            listaExemplosNeg.add(exNegConcept);
        }

        return listaExemplosNeg;
    }
    
    /**
     * Método para transformar o primeiro caracter de uma string em minusculo
     * 
     * @param s
     * @return lowerCaseFirstCharString
     */
    private String lowerFirstChar(String s) {
		String firstChar = s.substring(0, 1).toLowerCase();
		String complemento = s.substring(1, s.length());
		return firstChar+complemento;
	}

}
