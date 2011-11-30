
package br.uniriotec.orion.model.forte.resources;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Classe representante de um conceito para o FORTE.
 *
 * @author Felipe
 */
public class Concept extends ForteResource {
    private String nome;
    private List<ConceptAttribute> atributos = null;
    private List<ConceptAxiom> axiomas = null;
    private List<ConceptRestriction> restrictions = null;
    private boolean abstractConcept = false;

    /**
     * Sobrecargando metodo toString para forcar a escrita do Conceito como
     * um conceito do FORTE (Clausula de Horn). Se o conceito for abstrato,
     * i.e. nao possuir instancias declaradas na ontologia, sera gerado um 
     * conjunto de regras que explicite as subclasses do conceito.
     * 
     * OBS: como neste ponto ainda nao eh possivel saber quem esta no FDT e 
     * quem sera revisado, ainda nao eh possivel inserir o prefixo "fdt:" para 
     * os predicados que estao definidos no arquivo FDT.
     *
     * @return texto - String
     */
    @Override
    public String toString(){
        List<String> variaveis = new ArrayList<String>(Arrays.asList(getListaVariaveis()));
        String varPrincipal = variaveis.remove(0);
        String cabecaRegra = nome + "(" + varPrincipal + ") :-  "; 
        String corpoRegra = "";
        String regras = "";
        
        /*
         * Se o conceito for abstrato o metodo ira retornar simplesmente um conjunto
         * de regras onde cada uma faz simples referencia a uma das subclasses do conceito
         */
        if(abstractConcept){
        	if(axiomas != null){
                for(ConceptAxiom axioma : axiomas){
                    //Se for um axioma superClassOf
                    if(axioma.getNome().equals("superClassOf")){
                    	regras += cabecaRegra + axioma.getValor() + "(" + varPrincipal + "). \n";
                    }
                }
            }
        	return regras;
        }
        
        /*
         * Se o conceito NAO for abstrato o metodo retorna as regras que o definem, com 
         * base nos axiomas, seu s
         */
        
        //Adicionar AXIOMAS
        if(axiomas != null){
            for(ConceptAxiom axioma : axiomas){
            	//Se for um axioma subClassOf
            	//Este codigo eh responsavel por escrever conceitos negativos no FDT
                if(axioma.getNome().equals("subClassOf")){
                    if(nome.subSequence(0, 3).equals("nao")){
                        corpoRegra += axioma.getValor() + "("+ varPrincipal +"), ";
                    }
                }
                //Se for um axioma disjointWith
                if(axioma.getNome().equals("disjointWith")){
                	String axiomaDisjoint = axioma.getValor() + "(" + varPrincipal + "), ";
                	if(corpoRegra.contains(axiomaDisjoint) == false){
                		corpoRegra += axiomaDisjoint;
                	}
                }

                //Se for um axioma equivalentClass
                if(axioma.getNome().equals("equivalentClass")){
                    //TODO adicionar axiomas equivalentClass a clausula Horn
                }
            }
        }

        //Adicionar Restrictions
        if(restrictions != null){
            for(ConceptRestriction rest : restrictions){
                if(rest.getTipoRestriction().equals("hasValue")){
                    corpoRegra += rest.getNomeProperty() +
                            "(" + varPrincipal + "," + rest.getValorRestriction()+"), ";
                }

                if(rest.getTipoRestriction().equals("someValuesFrom")){
                    String varAux = variaveis.remove(0);
                    corpoRegra += rest.getNomeProperty() +
                            "(" + varPrincipal + "," + varAux + "), "+
                            rest.getValorRestriction() + "(" + varAux + "), ";
                }

                if(rest.getTipoRestriction().equals("cardinality")){
                    //TODO Como implementar??
                }

                if(rest.getTipoRestriction().equals("minCardinality")){
                    //TODO Como implementar??
                }

                if(rest.getTipoRestriction().equals("maxCardinality")){
                    //TODO Como implementar??
                }
            }
        }

        //Trocar a ultima virgula por um ponto
        int tamanhoTexto = corpoRegra.length();
        if(tamanhoTexto==0){
        	return cabecaRegra + "true.";
        }else{
	        return cabecaRegra + corpoRegra.substring(0, tamanhoTexto-2)+ ".";
        }
        
    }
    
    /**
     * Sobreescrita do método de comparação do objeto. Um objeto Concept é 
     * considerado igual a um outro objeto Concept quando seus nomes e corpo 
     * de regra são iguais.
     */
    @Override  
    public boolean equals(Object object) { 
    	//Verifica se o objeto recebido é um Concept, se não for o teste termina.
    	if(object instanceof Concept == false){
    		return false;
    	}
    	
    	Concept c = (Concept) object;
    	
    	//Verifica se o méetodo "toString()" gera a mesma String como regra.
    	if(this.toString().equals(c.toString())){
    		return true;
    	}else{
    		return false;
    	}
    	
    }
   

    /**
     * Adiciona um ConceptAtributo ao conceito.
     *
     * @param atributo - ConceptAttribute
     * @return boolean
     */
    public boolean addAttribute(ConceptAttribute atributo){
        if(atributos == null){
            atributos = new ArrayList<ConceptAttribute>();
        }

        atributos.add(atributo);
        return true;
    }

    /**
     * Adiciona um axioma ao conceito
     *
     * @param axioma
     * @return boolean
     */
    public boolean addConceptAxiom(ConceptAxiom axioma){
        //Verifica se o mapa está instanciado, se não estiver, instancia-o
        if(axiomas == null){
            axiomas = new ArrayList<ConceptAxiom>();
        }

        axiomas.add(axioma);
        return true;
    }

    /**
     * Adiciona uma restrição ao conceito.
     *
     * @param restriction
     * @return boolean
     */
    public boolean addConceptRestriction(ConceptRestriction restricao){
        //Verifica se o mapa está instanciado, se não estiver, instancia-o
        if(restrictions == null){
            restrictions = new ArrayList<ConceptRestriction>();
        }

        restrictions.add(restricao);
        return true;
    }





    /*************************************************
     *              Getters e Setters               **
     *************************************************/


    public List<ConceptAttribute> getAtributos() {
        return atributos;
    }

    public void setAtributos(List<ConceptAttribute> atributos) {
        this.atributos = atributos;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public List<ConceptAxiom> getAxiomas() {
        return axiomas;
    }

    public void setAxiomas(List<ConceptAxiom> axiomas) {
        this.axiomas = axiomas;
    }

    public List<ConceptRestriction> getRestrictions() {
        return restrictions;
    }

    public void setRestrictions(List<ConceptRestriction> restrictions) {
        this.restrictions = restrictions;
    }
    
    /**
     * Metodo para verificar se o conceito eh abstrato. Um conceito abstrat eh aquele que nao
     * pode ser instanciado, portanto, nao possui instancias declaradas na ontologia.
     */
	public boolean isAbstractConcept() {
		return abstractConcept;
	}

	public void setAbstractConcept(boolean abstractConcept) {
		this.abstractConcept = abstractConcept;
	}
	
	private String[] getListaVariaveis() {
        String[] variaveisArray = {"A","B","C","D","E","F","G","H","I","J","K",
                                    "L","M","N","O","P","Q","R","S","T","U","V",
                                    "W","X","Y","Z"};
        return variaveisArray;
    }
    
}
