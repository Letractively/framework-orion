
package br.uniriotec.orion.model.forte.resources;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Classe representante de um conceito para o FORTE.
 *
 * @author Felipe
 */
public class Concept extends ForteResource{
    private String nome;
    private List<ConceptAttribute> atributos = null;
    private List<ConceptAxiom> axiomas = null;
    private List<ConceptRestriction> restrictions = null;

    /**
     * Sobrecargasdo m√©todo toString para for√ßar a escrita do Conceito como
     * um conceito do FORTE (Cl√°usula Horn)
     *
     * @return texto - String
     */
    @Override
    public String toString(){
        List<String> variaveis = new ArrayList<String>(Arrays.asList(getListaVariaveis()));
        String varPrincipal = variaveis.remove(0);
        String texto = nome + "(" + varPrincipal + ") :- ";

        //Adicionar AXIOMAS
        if(axiomas != null){
            for(ConceptAxiom axioma : axiomas){
                //Se for um axioma subClassOf
                if(axioma.getNome().equals("subClassOf")){
                    texto += axioma.getValor() + "("+ varPrincipal +"), ";
                }

                //Se for um axioma disjointWith
                if(axioma.getNome().equals("disjointWith")){
                    texto += axioma.getValor() + "(" + varPrincipal + "), ";
                }

                //Se for um axioma equivalentClass
                if(axioma.getNome().equals("equivalentClass")){
                    //TODO adicionar axiomas equivalentClass √† cl√°usula Horn
                }
            }
        }

        //Adicionar Restrictions
        if(restrictions != null){
            for(ConceptRestriction rest : restrictions){
                if(rest.getTipoRestriction().equals("hasValue")){
                    texto += rest.getNomeProperty() +
                            "(" + varPrincipal + "," + rest.getValorRestriction()+"), ";
                }

                if(rest.getTipoRestriction().equals("someValuesFrom")){
                    String varAux = variaveis.remove(0);
                    texto += rest.getNomeProperty() +
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

        //Lembrar de tirar a √∫ltima v√≠rgula
        int tamanhoTexto = texto.length();
        texto = texto.substring(0, tamanhoTexto-2);
        texto += ".";
        
        return texto;
    }
    
    /**
     * Sobreescrita do mÈtodo de comparaÁ„o do objeto. Um objeto Concept È 
     * considerado igual a um outro objeto Concept quando seus nomes e corpo 
     * de regra s„o iguais.
     */
    @Override  
    public boolean equals(Object object) { 
    	//Verifica se o objeto recebido È um Concept, se n„o for o teste termina.
    	if(object instanceof Concept == false){
    		return false;
    	}
    	
    	Concept c = (Concept) object;
    	
    	//Verifica se o mÈetodo "toString()" gera a mesma String como regra.
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
        //Verifica se o mapa est√° instanciado, se n√£o estiver, instancia-o
        if(axiomas == null){
            axiomas = new ArrayList<ConceptAxiom>();
        }

        axiomas.add(axioma);
        return true;
    }

    /**
     * Adiciona uma restri√ß√£o ao conceito.
     *
     * @param restriction
     * @return boolean
     */
    public boolean addConceptRestriction(ConceptRestriction restricao){
        //Verifica se o mapa est√° instanciado, se n√£o estiver, instancia-o
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

    private String[] getListaVariaveis() {
        String[] variaveisArray = {"A","B","C","D","E","F","G","H","I","J","K",
                                    "L","M","N","O","P","Q","R","S","T","U","V",
                                    "W","X","Y","Z"};
        return variaveisArray;
    }
    
}
