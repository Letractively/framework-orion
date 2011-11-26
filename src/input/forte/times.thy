apelido_Time(A) :- fdt:refereseTime(A,B), time(B).
presidente(A) :- fdt:comandaClube(A,B), time(B).
torcida(A) :- fdt:torcePorTime(A,B), time(B).
time(A) :- 
apelido(A) :- apelido_Pessoa(A). 
apelido(A) :-  apelido_Time(A). 
apelido(A) :-  apelido_Torcida(A).
funcionario(A) :- presidente(A). 
funcionario(A) :-  membro_Comissao_Tecnica(A). 
funcionario(A) :-  tecnico(A).
apelido_Torcida(A) :- fdt:refereseTorcida(A,B), torcida(B).
membro_Comissao_Tecnica(A) :- fdt:trabalhaEm(A,B), time(B).
mascote(A) :- fdt:simboliza(A,B), time(B).
tecnico(A) :- fdt:trabalhaEm(A,B), time(B).
titular(A) :- fdt:jogaEmTime(A,B), time(B).
estadio(A) :- fdt:pertenceTime(A,B), time(B).
jogador_Aposentado(A) :- fdt:jogaEmTime(A,B), time(B).
patrocinador(A) :- fdt:patrocina(A,B), time(B).
torcedor(A) :- fdt:torcePorTime(A,B), time(B).
pessoa(A) :- funcionario(A). 
pessoa(A) :-  jogador(A). 
pessoa(A) :-  torcedor(A).
jogador(A) :- jogador_Aposentado(A). 
jogador(A) :-  titular(A).
apelido_Pessoa(A) :- fdt:referesePessoa(A,B), pessoa(B).
