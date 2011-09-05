ex_Jogador(A) :- jogador(A), naotitular(A).
apelido_Time(A) :- apelido(A), naoapelido_Torcida(A), refereseTime(A,B), Time(B).
presidente(A) :- funcionario(A), naotecnico(A), comandaClube(A,B), Time(B).
apelido(A) :- thing(A), naotorcida(A), naotime(A), naopessoa(A), naopatrocinador(A), naomascote(A), naoestadio(A).
torcida(A) :- thing(A), torcePorTime(A,B), Time(B).
membro_Comissao_Tecnica(A) :- funcionario(A), naotecnico(A), naopresidente(A), trabalhaEm(A,B), Time(B).
mascote(A) :- thing(A), naotorcida(A), naotime(A), naopessoa(A), naopatrocinador(A), simboliza(A,B), Time(B).
tecnico(A) :- funcionario(A), trabalhaEm(A,B), Time(B).
titular(A) :- jogador(A).
estadio(A) :- thing(A), naotorcida(A), naotime(A), naopessoa(A), naopatrocinador(A), naomascote(A), pertenceTime(A,B), Time(B).
apelido_Pessoa(A) :- apelido(A), naoapelido_Torcida(A), naoapelido_Time(A), referesePessoa(A,B), Pessoa(B).
apelido_Torcida(A) :- apelido(A), refereseTorcida(A,B), Torcida(B).
funcionario(A) :- pessoa(A).
jogador(A) :- pessoa(A), jogaEmTime(A,B), Time(B).
time(A) :- thing(A).
pessoa(A) :- thing(A), naotorcida(A), naotime(A).
patrocinador(A) :- thing(A), naotorcida(A), naotime(A), naopessoa(A), patrocina(A,B), Time(B).
torcedor(A) :- pessoa(A), torcePorTime(A,B), Time(B).
