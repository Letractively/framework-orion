
package br.uniriotec.orion.control;

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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

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
            tmpConcept.setNome(ontClass.getLocalName());
            //recuperar todos os Datatype properties utilizados pela OntClass
            conjDatatypes = parser.listarDatatypeProperties(ontClass);
            Iterator<DatatypeProperty> iterator = conjDatatypes.iterator();

            /* Para cada DatatypeProperty encontrado criar um objeto
             * ConceptAttribute e adiciona-lo a lista de atributos do Concept.
             */
            while(iterator.hasNext()){
                DatatypeProperty datatype = iterator.next();
                ConceptAttribute atrib = new ConceptAttribute();
                atrib.setNomeAtributo(datatype.getLocalName());
                atrib.setTipoRange(recuperarDomainsDatatype(datatype));
                //TODO Os atributos ainda não tem valores definidos, necessária
                //interação com o usuário
                tmpConcept.addAttribute(atrib);
            }

            /* Recupera todos os axiomas subClassOf da classe, identifica qual
             * faz referencia a classe pai e adiciona aos axiomas do conceito.
             *
             * Recupera todos os restrictions indicados nos subclassOf restantes
             * e adiciona como restrições no conceito atraves do método addRestriction
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
                       axioma.setValor(aux.getLocalName());
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
                    //TODO Não revisar este conceito
                    Concept conceitoNeg = new Concept();
                    ConceptAxiom axiomaNeg = new ConceptAxiom();
                    axiomaNeg.setNome("subClassOf");
                    axiomaNeg.setValor("not "+aux.getLocalName());
                    conceitoNeg.setNome("nao"+aux.getLocalName());
                    conceitoNeg.addConceptAxiom(axiomaNeg);

                    //Adiciona o conceito negativo à lista de conceitos
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
     * recupera todos os objetos ObjectProperty da ontologia e cria objetos
     * Relationship com base neles. É considerado o caso de Range se referir
     * a mais de uma Classe (fazendo uso de um UnionOf)
     *
     * @return List<Relationship>
     */
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

            //Adicionar Objeto à lista de retorno
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
     * (necessita de pós interação com usuário para preenchimento
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
                //TODO Adicionar à lista
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
        //TODO ainda não tem os possíveis valores. Talves retirar possibilidades das instancias ou das descrições dos  DatatypeProperties na OWL
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
     * <ol>Recuperar as classes da ontologia e utilizar instancias especificas para gerar exemplos
     * negativos de classes mais genéricas, ou seja, pegar uma instancia de uma subclasse e 
     * utilizá-la para gerar uma instancia da classe pai.</ol>
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
        for(OntClass classeOriginal : conjClasses){
            if(classeOriginal.getDisjointWith() != null){
                //iterador em cima de todas as classes disjuntas
                Iterator<OntClass> itClassesDisjuntas = classeOriginal.listDisjointWith();
                OntClass classeDisjunta = null;
                //Enquanto houver classe disjunta
                while(itClassesDisjuntas.hasNext()){
                    classeDisjunta = itClassesDisjuntas.next();
                    /**
                     * passar classe original e classe disjunta para gerar exemplos negativos
                     * para as duas. Adicionar todos os exemplos negativos ao conjunto de retorno.
                     */
                    listaExemplosNegativos.addAll(gerarExemplosDisjuntos(classeOriginal, classeDisjunta));
                }
            }
        }

        //////////////////////////////////////
        //          Heurística 2            //
        //////////////////////////////////////

        return listaExemplosNegativos;
    }

    /**
     * <p>method responsible for generating the top_level_predicates
     * in th .DAT file</p>
     */
    public String generateTopLevelPredicates(){

        Set<ObjectProperty> conjuntoOP = parser.listarObjectProperties();

        String topLevelPredicates = "top_level_predicates([";

        int cont = 0;
        for(ObjectProperty prop : conjuntoOP){
            cont++;

            String regra;

            String about = prop.getLocalName();
            String domain = prop.getDomain().getLocalName();
            String range = prop.getRange().getLocalName();

            regra = about + "(" + domain + "," + range + ")";
            topLevelPredicates += regra;

            if(cont != conjuntoOP.size()){
                topLevelPredicates += ", ";
            }else{
                topLevelPredicates += "]).";
            }
        }

        return topLevelPredicates;
    }



    /**
     * Generates the ontology rules and insert them on the .THY file
     */
    public void generateTheory(){
        //TODO todo conceito que possuir "nao" no inicio, deve ir para o FDT.
        //TODO todo conceito que possuir um axioma equivalentClass deve ir para o FDT.
        //TODO todo conceito que for citado por um equivalentClass deve ir para o FDT
    }














    /*********************************************************
     **                                                     **
     **                 Métodos Auxiliares                  **
     **                                                     **
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

}
