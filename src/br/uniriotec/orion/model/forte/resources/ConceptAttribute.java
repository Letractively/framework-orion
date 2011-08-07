
package br.uniriotec.orion.model.forte.resources;

import java.util.ArrayList;
import java.util.List;

/**<p>Classe que representa um atributo do objeto Classe. Classes são utilizadas
 * na representação em prolog para o FORTE e comportam atributos correspondentes
 * a Datatype Properties da OWL.</p>
 *
 * <p>OBS: Na representação OWL uma datatype Property indica a quais classes
 * está relacionada, na representação em prolog os conceitos informam seus
 * object_relations (o equivalente ao datatype property). Esta alteração de
 * pertinencia foi realizada para aproximar a implementação das classes Classe
 * e ConceptAttribute do esperado na representação para o FORTE.</p>
 *
 * @author Felipe
 */
public class ConceptAttribute extends ForteResource{
    private String nomeAtributo;
    private List<String> tipoRange;
    private List<String> valoresRange = new ArrayList<String>();

    @Override
    public String toString(){
        String texto = nomeAtributo + "([";
        for(String s : valoresRange){
            texto += s+", ";
            texto = texto.substring (0, texto.length() - 2);
        }
        texto += "])";

        return texto;
    }

    public String getNomeAtributo() {
        return nomeAtributo;
    }

    public void setNomeAtributo(String nomeAtributo) {
        this.nomeAtributo = nomeAtributo;
    }

    public List<String> getValoresRange() {
        return valoresRange;
    }

    public void setValoresRange(List<String> valoresRange) {
        this.valoresRange = valoresRange;
    }

    public List<String> getTipoRange() {
        return tipoRange;
    }

    public void setTipoRange(List<String> tipoRange) {
        this.tipoRange = tipoRange;
    }
}
