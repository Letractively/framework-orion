/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package br.uniriotec.orion.model.forte.resources;

/**Classe para representar uma restrição em um conceito.
 *
 * @author Felipe
 */
public class ConceptRestriction extends ForteResource {
    private String nomeProperty = "naodefinido";
    private String tipoRestriction = "naodefinido";
    private String valorRestriction = "naodefinido";

    @Override
    public String toString(){
        return "Property: " + nomeProperty + ", " + tipoRestriction + ": " + valorRestriction;
    }

    public String getNomeProperty() {
        return nomeProperty;
    }

    public void setNomeProperty(String nomeProperty) {
        this.nomeProperty = nomeProperty;
    }

    public String getTipoRestriction() {
        return tipoRestriction;
    }

    public void setTipoRestriction(String tipoRestriction) {
        this.tipoRestriction = tipoRestriction;
    }

    public String getValorRestriction() {
        return valorRestriction;
    }

    public void setValorRestriction(String valorRestriction) {
        this.valorRestriction = valorRestriction;
    }
}
