package movil;


public class equipo {
  private Vector dni, nombre, situacion, amonestaciones, goles;

  private String nombre_e;
  private String tipo;
  private int golesPropiaPuerta;

  public equipo(String nombre_, String tipo_) {
    nombre_e = nombre_;
    tipo = tipo_;
    dni = new Vector();
    nombre = new Vector();
    situacion = new Vector();
    amonestaciones = new Vector();
    goles = new Vector();
    this.golesPropiaPuerta=0;
    System.out.println("goles en propia puerta: "+this.getPP());
  }

public void consolaEquipo(){
  System.out.println("Nombre: "+getNombre());
  System.out.println("Tamaño: "+this.getTamaño());
  System.out.println("Tipo: "+getTipo());

    for (int i=0;i<this.getTamaño();i++)
    System.out.println(getNombreJugador(i)+" "+getSituacionJugador(i)+" "+getGol(i)+" "+getAmonestacion(i));
}
  public String getNombre() {
    return this.nombre_e;
  }

  public int getTamaño() {
    return this.dni.size();
  }

  public String getTipo() {
    return this.tipo;
  }

  public int getPP(){
    return this.golesPropiaPuerta;
  }

  public void setPP(int goles){
    System.out.println("Modificamos los goles a"+goles);
    this.golesPropiaPuerta=goles;
  }

  public void setJugador(String dni_, String nombre_, String situacion_, String gol_, String amon_) {
    if (!estaYa(dni_)){
      dni.addElement(dni_);
      nombre.addElement(nombre_);
      situacion.addElement(situacion_);
      amonestaciones.addElement(amon_);
      goles.addElement(gol_);
    }
  }

  //evitamos que se metan duplicados en la tabla de jugadores
  public boolean estaYa(String dni_){
    for (int i=0; i<getTamaño();i++)
      if (getDniJugador(i).startsWith(dni_))
        return true;

    return false;

  }

  public String getDniJugador(int indice) {
    return (String) dni.elementAt(indice);
  }

  public String getNombreJugador(int indice) {
    return (String) nombre.elementAt(indice);
  }

  public boolean getSituacionJugador(int indice) {
    String aux = (String) situacion.elementAt(indice);
    if (aux.startsWith("S")) {
      return true;
    }
    return false;
  }
  public String getConvocadoNo(int indice){
        return ((String) situacion.elementAt(indice));
  }

  public void setSituacion(int jugador, String estado) {
    situacion.setElementAt(estado, jugador);
  }

  public void setAmonestacion(int jugador, String amonesta) {
    amonestaciones.setElementAt(amonesta, jugador);
  }

  public void setGol(int jugador, String cantidad) {
    goles.setElementAt(cantidad, jugador);
  }

  public String getGol(int indice) {
    return (String) goles.elementAt(indice);
  }

  public String getAmonestacion(int indice) {
    return (String) amonestaciones.elementAt(indice);
  }

  public boolean convocatoriaRellenada(){
    for (int i=0; i<getTamaño();i++)
      if (((String)situacion.elementAt(i)).startsWith("S"))
        return true;

    return false;
  }
}
