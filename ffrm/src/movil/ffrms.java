package movil;

//principal y validar son intocables tanto aqu� como en el c�digo de bbdd

public class ffrms
    extends MIDlet
    implements CommandListener, Runnable {
  //variables desde BBDD
  private String fecha = null;
  private int contador = 0;
  private String version = "1.1";

  //hilo de navegacion
  private Thread hiloNavegacion;

//pantallas
  private Pantalla principal;
  private Form pantallaDatos;
  private List pantallaMenu;

  private Display display;

//comandos
  private Command exitCommand, inicioCommand, iniciovCommand, goCommand,
      volverCommand, volverOfflineCommand, enviarTarCommand, enviarConCommand,
      enviarGolCommand, enviarTodoCommand, jugadorCommand, siCommand, noCommand;

//variables
  private equipo local, visitante;
  private Vector amoDesc = new Vector();
  static private Stack historial = new Stack();
  static private Vector parametrosEntrada = new Vector();
  static boolean res = false; //variable que indica si estamos en reservas
  private boolean primeraConsulta = true;
  private boolean soloEnviar = false;
  private Ticker tick;
  private String correo;
  private String pw;

//tratamiento urls
  static private Vector urls = new Vector();
  String urlBase;

  String url;
  String urlDeCambio;
  String urlVolver;
  String urlInicioPartido;

  public ffrms() {
    display = Display.getDisplay(this);
    exitCommand = new Command("Salir", Command.EXIT, 2);
    inicioCommand = new Command("Inicio", Command.SCREEN, 1);
    enviarTarCommand = new Command("Guardar", Command.SCREEN, 1);
    enviarTodoCommand = new Command("Enviar", Command.SCREEN, 1);
    enviarConCommand = new Command("Guardar", Command.SCREEN, 1);
    enviarGolCommand = new Command("Guardar", Command.SCREEN, 1);
    iniciovCommand = new Command("Aceptar", Command.SCREEN, 1);
    goCommand = new Command("Siguiente", Command.SCREEN, 1);
    jugadorCommand = new Command("Nuevo", Command.SCREEN, 2);
    volverCommand = new Command("Volver", Command.BACK, 2);
    volverOfflineCommand = new Command("Volver", Command.BACK, 2);
    siCommand = new Command("Si", Command.SCREEN, 1);
    noCommand = new Command("No", Command.BACK, 2);
    rellenarAmonestaciones();
  }

  public String encripta(String cad) {
    String aux = new String("f");
    for (int i = 0; i < cad.length(); i++) {
      char temp = cad.charAt(cad.length() - i - 1);
      temp += 2;
      aux = aux + temp;
    }
    aux += "8";
    return aux;
  }

  public void validar() {
    pantallaDatos = new Form("");
    TextField aux;
    TextField aux2;
    aux = new TextField("Colegiado", "", 20,
                        TextField.NUMERIC);
    aux2 = new TextField("Password", "", 10,
                         TextField.NUMERIC);
    ImageItem img = new ImageItem("", this.montarImagen("pw"),
                                  ImageItem.LAYOUT_CENTER, "Password");
    pantallaDatos.append(aux);
    pantallaDatos.append(aux2);
    pantallaDatos.append(img);
    pantallaDatos.addCommand(iniciovCommand);
    pantallaDatos.addCommand(exitCommand);
    pantallaDatos.setCommandListener(this);
    display.setCurrent(pantallaDatos);
  }

  public void valida(String aux, String aux2) {
    correo = aux;
    pw = aux2;
    url = urlBase + "validar?dcorreo=" + aux + "&pw=" + encripta(aux2)+ "&version=" + version;
    rulaHilo();
  }

  public void inicio() {
    principal = new Pantalla("logo");
    principal.addCommand(inicioCommand);
    principal.addCommand(exitCommand);
    principal.setCommandListener(this);
    display.setCurrent(principal);
  }

  public void commandAction(Command c, Displayable s) {
    if (c == pantallaMenu.SELECT_COMMAND || c == goCommand) {
      siguientePantalla();
    }
    else if (c == exitCommand) {
      destroyApp(true);
      notifyDestroyed();
    }
    else if (c == iniciovCommand) {
      //hacemos la primera consulta y all� recuperamos login y password
      rulaHilo();
    }
    else if (c == inicioCommand) {
      this.historial.removeAllElements();
      if (fecha == null) {
        System.out.println("InicioCommand: fecha=null");
        validar();
        return;
      }
      else if (urlInicioPartido != null) {
                System.out.println("InicioCommand: !=null");
        url = urlBase + urlInicioPartido;
        urlInicioPartido = null;
      }
      else {
                System.out.println("InicioCommand: else");
        url = urlBase + "principal?dcorreo=" + correo + "&pw=" + encripta(pw);
      }
      System.out.println("Url formada por inicioCommand "+url);
      rulaHilo();
    }
    else if (c == jugadorCommand) {
      url = urlBase + urls.elementAt(0);
      rulaHilo();
    }
    else if (c == enviarTodoCommand) {
      pantallaEspera("Guardando");
      boolean algunError = false;
      //enviamos el resumen al completo del partido.
      String urlEnviarTodo = (String) urls.elementAt(0);
      url = urlBase + urlEnviarTodo + "&localpp=" + local.getPP() +
          "&visitantepp=" + visitante.getPP() + "&todo=";

      //amonestaciones y goles
      for (int i = 0; i < local.getTama�o(); i++) {
        url = url + local.getTipo() + "$" + local.getDniJugador(i) + "$" +
            local.getConvocadoNo(i) + "$" +
            local.getAmonestacion(i) + "$" + local.getGol(i) + "$";
      }

      for (int i = 0; i < visitante.getTama�o(); i++) {
        url = url + visitante.getTipo() + "$" + visitante.getDniJugador(i) +
            "$" + visitante.getConvocadoNo(i) + "$" +
            visitante.getAmonestacion(i) + "$" + visitante.getGol(i) + "$";
      }
      try {
        this.soloEnviar = true;
        Thread hiloVis = new Thread(this);
        hiloVis.start();
      }
      catch (Exception e) {
        algunError = true;
        e.printStackTrace();
      }

      if (algunError) {
        this.muestraMensaje("No se han podido guardar todos los datos");
      }
      else {
        this.muestraMensaje("Resumen del partido guardado correctamente");
      }

    }
    else if (c == enviarTarCommand) {
      enviarAmonestaciones();
    }
    else if (c == enviarConCommand) {
      enviarConvocatoria();
    }
    else if (c == enviarGolCommand) {
      enviarGoles();
    }
    else if (c == volverOfflineCommand) {
      inicio();
    }
    else if (c == volverCommand) {
      if (url.indexOf("menu_partido") != -1) {
        //en este caso estamos en el menu partido y vamos a salirnos del partido al menu principal
        avisarSalida();
        return;
      }
      else {
        procVolver();
      }
    }
    else if (c == siCommand) {
      procVolver();
    }
    else if (c == noCommand) {
      rulaHilo();
    }
  }

  /*procedimiento que vuelve a la pantalla anterior de la que nos encontramos*/
  public void procVolver() {
    try {
      url = (String) historial.pop();
    }
    catch (Exception e) {
      if (fecha == null) {
        //caso en el que falla la validacion
        validar();
        return;
      }
      else {
        url = urlBase + "principal?dcorreo=" + correo + "&pw=" + encripta(pw);
      }
    }
    rulaHilo();
  }

  private void avisarSalida() {
    Form pantallaAviso = new Form("");
    ImageItem img = new ImageItem("", this.montarImagen("mje"),
                                  ImageItem.LAYOUT_CENTER, "Mensaje");
    StringItem cadena = new StringItem("�Esta seguro de que desea salir?",
                                       "Se perderan los cambios no guardados");
    StringItem aux = new StringItem(" ", " ");
    pantallaAviso.append(aux);
    pantallaAviso.append(img);
    pantallaAviso.append(cadena);
    pantallaAviso.addCommand(siCommand);
    pantallaAviso.addCommand(noCommand);
    pantallaAviso.setCommandListener(this);
    display.setCurrent(pantallaAviso);
  }

  private void rulaHilo() {
    hiloNavegacion = new Thread(this);
    hiloNavegacion.start();
  }

  private void siguientePantalla() {
    /*A�adimos la url al historial*/
    if (urlDeCambio == null) {
      historial.push(url);
    }
    else {
      historial.push(urlDeCambio);
      urlDeCambio = null;
    }

    if (urls.isEmpty()) {
      String aux = pantallaMenu.getString(pantallaMenu.getSelectedIndex());
      url = urlBase + aux.replace(' ', '_');
    }
    else {
      if (res) {
        res = false;
        String aux = pantallaMenu.getString(pantallaMenu.getSelectedIndex());
        String aux2 = tratamiento(aux.substring(aux.indexOf(" ") + 1,
                                                aux.length()));
        url = urlBase + urls.elementAt(0) + aux2;
      }
      else if (pantallaMenu.getSelectedIndex() < 0) {
        url = urlBase + urls.elementAt(0);
      }
      else {
        url = urlBase + urls.elementAt(pantallaMenu.getSelectedIndex());
      }
    }
    if (!this.parametrosEntrada.isEmpty()) {
      if (url.indexOf("?") < 0) {
        url = url + "?";
      }
      for (int i = 0; i < parametrosEntrada.size(); i++) {
        if (url.indexOf("?") != url.length() - 1) {
          url = url + "&";

        }
        url = url + (String) parametrosEntrada.elementAt(i);
        url = url + "=" +
            valorParametro(pantallaDatos.get(i / 2),
                           Integer.
                           parseInt( (String) parametrosEntrada.elementAt(++i)));
      }
      url = url.replace(' ', '_');
    }
    rulaHilo();
  }

  /**solo se utiliza en las reservas*/
  public String tratamiento(String aux) {
    String devolver = null;
    if (aux.indexOf(":") > 0) {
      devolver = new String(aux.substring(0, 2));
      devolver = devolver + aux.substring(3, 5);
    }
    else if (aux.indexOf("/") > 0) {
      return aux;
    }
    else {
      devolver = aux;
    }
    return devolver;
  }

  public String valorParametro(Item item, int tipo) {
    if (tipo == 0) {
      //tipo combo
      ChoiceGroup aux = (ChoiceGroup) item;
      return aux.getString(aux.getSelectedIndex());
    }
    else if (tipo == 1) {
      //tipo caja de texto
      TextField aux = (TextField) item;
      return aux.getString();
    }
    else if (tipo == 2) {
      //tipo date;
      DateField aux = (DateField) item;
      Date fecha = aux.getDate();
    }
    else if (tipo == 3) {
      Gauge aux = (Gauge) item;
      return Integer.toString(aux.getValue());
    }

    System.out.println("Se ha producido un error en valorParametro()");
    return null;
  }

  protected void memorias() {
    System.out.println("----------------------------------------------------");
    System.out.println("Estado de la memoria urls: ");
    for (int i = 0; i < urls.size(); i++) {
      System.out.println(i + ": " + urls.elementAt(i));
    }

    System.out.println("Estado de la memoria parametrosEntrada: ");
    for (int i = 0; i < parametrosEntrada.size(); i++) {
      System.out.println(i + ": " + parametrosEntrada.elementAt(i));
    }
    System.out.println("Historial: " + historial.size() + " " +
                       historial.firstElement());
    System.out.println("----------------------------------------------------");
  }

  protected void memoriaa(Vector aux) {
    System.out.println("Estado de los argumentos: ");
    for (int i = 0; i < aux.size(); i++) {
      System.out.println(i + ": " + aux.elementAt(i));
    }
    System.out.println("----------------------------------------------------");
  }

//--------------------------------------------------------------

  public void startApp() {
    inicio();
  }

  public void pauseApp() {}

  public void destroyApp(boolean unconditional) {

  }

  /*Metodo que arranca el hilo y comienza una nueva tuberia*/
  public void run() {
    try {
      if (primeraConsulta) {
        pantallaEspera("Cargando"); //mientras que envia informacion le decimos al usuario que aguante jejej
        this.leerFicheroConfiguracion();
      }
      else if (soloEnviar) {
        soloEnviar = false;
        this.soloEnvio(url);
      }
      else {
        pantallaEspera("Cargando"); //mientras que envia informacion le decimos al usuario que aguante jejej
        verServicios(url);
      }
    }
    catch (IOException e) {
      e.printStackTrace();
      muestraError(e);
    }
  }

  private void pantallaEspera(String mensaje) {
    Form formEspera = new Form("");
    ImageItem t = new ImageItem("", montarImagen("espera"),
                                ImageItem.LAYOUT_CENTER, "");
    StringItem aux = new StringItem("", "     " + mensaje + "...");
    formEspera.append(t);
    formEspera.append(aux);
    display.setCurrent(formEspera);
  }

  /**Procedimiento que muestra todas las opciones de la aplicacion*/
  private void muestraInstrucciones() {
    Form formInstru = new Form("");
    StringItem cadena = new StringItem("inicio: ", "Comienza la navegacion");
    StringItem cadena3 = new StringItem("go: ", "Aceptar/Entrar");
    StringItem cadena4 = new StringItem("volver: ", "Pantalla principal");
    StringItem cadena2 = new StringItem("salir: ", "Cierra el programa");

    tick = new Ticker("Opciones que encontrara en la aplicacion");
    formInstru.setTicker(tick);
    formInstru.append(cadena);
    formInstru.append(cadena3);
    formInstru.append(cadena4);
    formInstru.append(cadena2);
    formInstru.addCommand(volverOfflineCommand);
    formInstru.setCommandListener(this);
    display.setCurrent(formInstru);
  }

  /**Coge los par�metros necesarios para la ejecuci�n dela aplicacion de la direccion
   * que se indica en -URL-. Da igual que sea get...*/
  public void leerFicheroConfiguracion() throws IOException {
    url = "http://www.ffrm.es/ffrm/conf.xml";
//    url = "http://www.ffrm.es/ffrmtest/confpre.xml";
    HttpConnection c = null;
    InputStream is = null;
    StringBuffer b = new StringBuffer();
    int ch;
    try {
      c = (HttpConnection) Connector.open(url);
      c.setRequestMethod(HttpConnection.GET);
      c.setRequestProperty("User-Agent",
                           "Profile/MIDP-1.0 Configuration/CLDC-1.0");
      c.setRequestProperty("Content-Language", "es-ES");
      is = c.openInputStream();
// leer----------------------------------------------------------------------
      while ( (ch = is.read()) != -1) {
        contador++;
        b.append( (char) ch);
        if (ch == '\n') {
          if (b.toString().startsWith("<urb>")) {
            urlBase = extraer(b);
            System.out.println("urb fichero " + url + " url leida " + urlBase);
            this.primeraConsulta = false;
          }
          else if (b.toString().startsWith("<urf>")) {
            urlBase = extraer(b);
            this.primeraConsulta = false;
            System.out.println("urf " + url + " url leida " + urlBase);
          }
        }
      }
      if (primeraConsulta == false) {
        valida( ( (TextField) pantallaDatos.get(0)).getString(),
               ( (TextField) pantallaDatos.get(1)).getString());
      }
      else {
        this.muestraError("Fichero de configuraci�n err�neo");
      }
    }
    catch (Exception e) {
      this.muestraError(
          "No se puede establecer conexion leyendo el fichero de configuracion");
    }
    finally {
      //variables de la conexion
      if (is != null) {
        is.close();
      }
      if (c != null) {
        c.close();
      }
    }
  }

  public String urlSinParametros(String aux) {
    try {
      int h = aux.indexOf("?");
      return aux.substring(0, h);
    }
    catch (Exception e) {
      return aux;
    }
  }

  public String parametrosUrl(String aux) {
    if (aux.indexOf("?") < 0) {
      return null;
    }
    else {
      int h = aux.indexOf("?") + 1;
      String aux2 = aux.substring(h, aux.length());
      return aux2.replace(' ', '_');
    }
  }

  /**Procedimiento que muestra todos los jugadores de una plantilla
   * le especificamos en el par�metro el tipo de dato que queremos
   * acompa�ar a la plantilla*/
  public void mostrarPlantilla(int tipo) {
    //tipo 1 amonestraciones local
    //tipo 2 goles local
    //tipo 3 fotos local
    //tipo 5 amonestraciones visitante
    //tipo 6 goles visitante
    //tipo 7 fotos visitante

    equipo plantilla;
    if (tipo < 5) {
      plantilla = local;
    }
    else {
      plantilla = visitante;
    }
    //Comprobamos que se ha introducido la convocatoria para introducir goles y amonestaciones
    if ( (tipo == 2 || tipo == 6 || tipo == 1 || tipo == 5) &&
        (!plantilla.convocatoriaRellenada())) {
      this.muestraMensaje("Debe establecer la convocatoria primero");
      return;
    }

    try {
      pantallaDatos = new Form(plantilla.getNombre());
      if (tipo == 1 || tipo == 5) {
        this.tick = new Ticker("Indique las amonestaciones");
        pantallaDatos.addCommand(volverCommand);
        pantallaDatos.addCommand(this.enviarTarCommand);
      }
      else if (tipo == 4 || tipo == 8) {
        this.tick = new Ticker("Indique la convocatoria: " +
                               plantilla.getNombre());
        pantallaDatos.addCommand(volverCommand);
        pantallaDatos.addCommand(jugadorCommand);
        pantallaDatos.addCommand(this.enviarConCommand);
      }
      else if (tipo == 2 || tipo == 6) {
        this.tick = new Ticker("Indique los goles");
        pantallaDatos.addCommand(volverCommand);
        pantallaDatos.addCommand(this.enviarGolCommand);
      }
      else if (tipo == 3 || tipo == 7) {
        this.tick = new Ticker("Revision de fichas");
        pantallaDatos.addCommand(volverCommand);
      }
      pantallaDatos.setTicker(tick);

      //establecemos el tipo de pantalla que se mostrar� al usuario
      for (int i = 0; i < plantilla.getTama�o(); i++) {
        //en el caso de amonestaciones
        if (tipo == 1 || tipo == 5) {
          if (plantilla.getSituacionJugador(i)) {
            ChoiceGroup t = new ChoiceGroup(plantilla.getDniJugador(i) + " " +
                                            plantilla.getNombreJugador(i),
                                            ChoiceGroup.POPUP);
            //obtenemos el codigo de la amonestaci�n
            int aux = Integer.parseInt( (String) plantilla.getAmonestacion(i));
            //la metemos como seleccionada
            t.append( (String) amoDesc.elementAt(aux), null);
            //metremos el resto de motivos quitando el que ya hemos metido
            for (int j = 0; j < amoDesc.size(); j++) {
              if (j != aux) {
                t.append( (String) amoDesc.elementAt(j), null);
              }
            }
            pantallaDatos.append(t);
          }
        }
        //caso de los goles
        else if (tipo == 2 || tipo == 6) {
          if (plantilla.getSituacionJugador(i)) {
            TextField t = new TextField(plantilla.getDniJugador(i) + " " +
                                        plantilla.getNombreJugador(i),
                                        plantilla.getGol(i), 2,
                                        TextField.NUMERIC);
            pantallaDatos.append(t);
          }
          //si es el �ltimo jugador a�adimos tb los goles en pp del equipo contrario
          if (i + 1 == plantilla.getTama�o()) {
            equipo plantillaAux = local;
            if (plantilla == local) {
              plantillaAux = visitante;
            }
            TextField t = new TextField("En propia puerta " +
                                        plantillaAux.getNombre(),
                                        Integer.toString(plantillaAux.
                getPP()), 2,
                                        TextField.NUMERIC);
            pantallaDatos.append(t);
          }
        }
        //convocados
        else if (tipo == 4 || tipo == 8) {
          Image aux = ffrms.montarImagen("ok");
          Image aux2 = ffrms.montarImagen("no");
          Image[] imagenes = {
              aux2, aux};
          Image[] imagenes2 = {
              aux, aux2};
          String[] situacion = {
              "No Convocado", "Convocado"};
          String[] situacion2 = {
              "Convocado", "No Convocado"};
          ChoiceGroup t;
          if (plantilla.getSituacionJugador(i)) {
            t = new ChoiceGroup(plantilla.getDniJugador(i) + " " +
                                plantilla.getNombreJugador(i),
                                ChoiceGroup.POPUP, situacion2,
                                imagenes2);
          }
          else {
            t = new ChoiceGroup(plantilla.getDniJugador(i) + " " +
                                plantilla.getNombreJugador(i),
                                ChoiceGroup.POPUP, situacion,
                                imagenes);
          }
          pantallaDatos.append(t);
        }
        /*                 IMAGENES
             //en otro caso sacamos imagenes
                 else if (tipo == 3 || tipo == 7) {
         StringItem str = new StringItem("Nombre: ", plantilla.getDniJugador(i));
          pantallaDatos.append(str);
          Image im = getImage( (String) plantilla.elementAt(++i));
          if (im != null) {
            pantallaDatos.append(im);
          }
          else {
            StringItem aux = new StringItem(" ", "Foto no disponible");
            pantallaDatos.append(aux);
          }
                 }
         */
      }
      pantallaDatos.setCommandListener(this);
      display.setCurrent(pantallaDatos);
    }
    catch (Exception ex) {
      System.out.println(ex);
    }
  }

  /*Procedimiento que guarda los convocados de un partido*/
  public void enviarConvocatoria() {
    equipo plantilla;
    if (pantallaDatos.getTicker().getString().indexOf(local.getNombre()) > 0) {
      plantilla = local;
    }
    else {
      plantilla = visitante;

      //son los mismos en pantalla que en la tabla.
    }
    int cuantos = pantallaDatos.size();
    String urlConvocar = ( (String) urls.firstElement());
    for (int i = 0; i < cuantos; i++) {
      //sacamos lo que ha seleccionado el colegiado
      String aux = ( (ChoiceGroup) pantallaDatos.get(i)).getString( ( (
          ChoiceGroup) pantallaDatos.get(i)).getSelectedIndex());

      String estado = "N";
      if (aux.startsWith("Convocado")) {
        estado = "S";
      }

      //en local
      plantilla.setSituacion(i, estado);

      //en bbdd
      /*      url = urlBase + urlConvocar + "&equipo=" + plantilla.getTipo() +
       "&codigo_jugador=" + plantilla.getDniJugador(i) + "&estado=" + estado;
            try {
              soloEnvio(url);
            }
            catch (Exception e) {
              e.printStackTrace();
              muestraError("Fallo hilo envioConvocatoria");
            }*/
    }
    this.muestraMensaje("Convocatoria " + plantilla.getNombre() +
                        " temporalmente guardada");
  }

  public void resumenPartido() {
    Form datos = new Form("Resumen");
    tick = new Ticker("");
    datos.setTicker(tick);

    StringItem i = new StringItem("GOLEADORES ", local.getNombre());
    datos.append(i);
    int golesLocal = 0, golesVisi = 0;
    for (int index = 0; index < local.getTama�o(); index++) {
      if ( (Integer.parseInt(local.getGol(index)) != 0) &&
          (local.getSituacionJugador(index))) {
        StringItem j = new StringItem(local.getDniJugador(index) + " " +
                                      local.getNombreJugador(index),
                                      local.getGol(index));
        golesLocal += Integer.parseInt(local.getGol(index));
        datos.append(j);
      }
    }
    //incluimos los goles en propia puerta del visitante
    if (visitante.getPP() > 0) {
      StringItem j = new StringItem("En propia puerta " + visitante.getNombre(),
                                    Integer.toString(visitante.getPP()));
      golesLocal += visitante.getPP();
      datos.append(j);
    }
    i = new StringItem("GOLEADORES ", visitante.getNombre());
    datos.append(i);
    for (int index = 0; index < visitante.getTama�o(); index++) {
      if ( (Integer.parseInt(visitante.getGol(index)) != 0) &&
          (visitante.getSituacionJugador(index))) {
        StringItem j = new StringItem(visitante.getDniJugador(index) + " " +
                                      visitante.getNombreJugador(index),
                                      visitante.getGol(index));
        golesVisi += Integer.parseInt(visitante.getGol(index));
        datos.append(j);
      }
    }
    //incluimos los goles en pp del local
    if (local.getPP() > 0) {
      StringItem j = new StringItem("En propia puerta " + local.getNombre(),
                                    Integer.toString(local.getPP()));
      golesVisi += local.getPP();
      datos.append(j);
    }

    Ticker aux = datos.getTicker();
    aux.setString(local.getNombre() + " " + golesLocal + " - " +
                  golesVisi + " " + visitante.getNombre());
    i = new StringItem("AMONESTADOS ", local.getNombre());
    datos.append(i);
    for (int index = 0; index < local.getTama�o(); index++) {
      if ( (!local.getAmonestacion(index).startsWith("0")) &&
          (local.getSituacionJugador(index))) {
        StringItem j = new StringItem(local.getDniJugador(index) + " " +
                                      local.getNombreJugador(index),
                                      (String) amoDesc.elementAt(Integer.
            parseInt(local.getAmonestacion(index))));
        datos.append(j);
      }
    }
    i = new StringItem("AMONESTADOS ", visitante.getNombre());
    datos.append(i);
    for (int index = 0; index < visitante.getTama�o(); index++) {
      if ( (!visitante.getAmonestacion(index).startsWith("0")) &&
          (visitante.getSituacionJugador(index))) {
        StringItem j = new StringItem(visitante.getDniJugador(index) + " " +
                                      visitante.getNombreJugador(index),
                                      (String) amoDesc.elementAt(Integer.
            parseInt(visitante.getAmonestacion(index))));
        datos.append(j);
      }
    }
    datos.addCommand(enviarTodoCommand);
    datos.addCommand(volverCommand);
    datos.setCommandListener(this);
    display.setCurrent(datos);
  }

  public void rellenarAmonestaciones() {
    amoDesc.addElement("Sin amonestar");
    amoDesc.addElement("Amarilla");
    amoDesc.addElement("DobleAmarilla");
    amoDesc.addElement("Roja Directa");
    amoDesc.addElement("Amarilla y Roja");
  }

  /**Envia una a una las amonestaciones del partido*/
  public void enviarAmonestaciones() {
    equipo plantilla = visitante;
    if (pantallaDatos.getTitle().startsWith(local.getNombre())) {
      plantilla = local;

    }
    String urlEnviar = ( (String) urls.firstElement());
    //en pantalla hay menos que en la tabla, ya que aqui solo figuran los convocados
    int cuantos = plantilla.getTama�o();
    //indice pantalla
    int indiceConvocados = 0;

    for (int indicePlantilla = 0; indicePlantilla < cuantos; indicePlantilla++) {
      if (plantilla.getSituacionJugador(indicePlantilla)) {
        //sacamos lo que ha seleccionado el colegiado
        int seleccionN = ( (ChoiceGroup) pantallaDatos.get(indiceConvocados)).
            getSelectedIndex();
        String seleccionS = ( (ChoiceGroup) pantallaDatos.get(indiceConvocados)).
            getString(seleccionN);
        for (int i = 0; i < amoDesc.size(); i++) {
          if ( ( (String) amoDesc.elementAt(i)).compareTo(seleccionS) == 0) {
            seleccionN = i;
          }
        }
        indiceConvocados++;
        //en local
        plantilla.setAmonestacion(indicePlantilla, Integer.toString(seleccionN));
        //en bbdd
        /*        url = urlBase + urlEnviar + "&equipo=" + plantilla.getTipo() +
         "&codigo_jugador=" + plantilla.getDniJugador(indicePlantilla) +
                    "&amonestacion=" + seleccionN;
                try {
                  soloEnvio(url);
                }
                catch (Exception e) {
                  e.printStackTrace();
                          muestraError("Fallo hilo envioAmonestaciones");
                }*/
      }
    }
//    plantilla.consolaEquipo();
    this.muestraMensaje("Amonestaciones temporalmente guardadas");
  }

  /**Envia uno a uno los goleadores del partido*/
  public void enviarGoles() {
    equipo plantilla = visitante;
    if (pantallaDatos.getTitle().startsWith(local.getNombre())) {
      plantilla = local;
    }
    //en pantalla hay menos que en la tabla, ya que aqui solo figuran los convocados
    //indice pantalla
    int indiceConvocados = 0;

    //recorremos la pantalla y recogemos los goles introducidos por el usuario
    for (int indicePlantilla = 0; indicePlantilla < plantilla.getTama�o();
         indicePlantilla++) {
      if (plantilla.getSituacionJugador(indicePlantilla)) {
        //sacamos los que ha introducido el colegiado
        String aux = ( (TextField) pantallaDatos.get(indiceConvocados)).
            getString();
        indiceConvocados++;
        //en local
        plantilla.setGol(indicePlantilla, aux);
      }
        //si estamos en el �ltimo jugador nos disponemos a
        //tratar los goles en propia puerta
        if (indicePlantilla + 1 == plantilla.getTama�o()) {
          equipo plantillaAux = local;
          if (plantilla == local) {
            plantillaAux = visitante;
          }
          String aux = ( (TextField) pantallaDatos.get(pantallaDatos.size()-1)).
              getString();
          plantillaAux.setPP(Integer.parseInt(aux));
        }
    }
    this.muestraMensaje("Goles temporalmente guardados");
  }

  /**Devuelve una imagen desde la url especificada*/
  private Image getImage(String url) throws IOException {
    int contador = 0;
    ContentConnection connection = (ContentConnection) Connector.open(url);
    DataInputStream iStrm = connection.openDataInputStream();
    Image im = null;
    int ch;
    try {
      byte imageData[];
      ByteArrayOutputStream bStrm = new ByteArrayOutputStream();
      while ( (ch = iStrm.read()) != -1) {
        bStrm.write(ch);
        contador++;
      }
      System.out.println("bytes de la imagen: " + contador);
      imageData = bStrm.toByteArray();
      bStrm.close();
      im = Image.createImage(imageData, 0, imageData.length);
    }
    finally {
      if (iStrm != null) {
        iStrm.close();
      }
      if (connection != null) {
        connection.close();
      }
    }
    return (im == null ? null : im);
  }

  public boolean comprobarTodasRestricciones() {
    //comprobamos que almenos hayan 7 convocados por equipo
    //comprobamos que los goleadores se corresponden con el resultado
    return false;
  }

  /**Envia datos para guardar en bbdd, no genera nuevas pantallas*/
  public boolean soloEnvio(String url) throws IOException {
    boolean error = false;
    HttpConnection c = null;
    InputStream is = null;
    StringBuffer b = new StringBuffer();
    int ch;
    try {
      /*----------------TRATAMIENTO DE LOS PAR�METROS ------*/
      System.out.println("Soloenvio: " + url);
      String params = this.parametrosUrl(url);
      url = this.urlSinParametros(url);
      /*-----------------------------------------------------------------*/
      OutputStream os = null;
      c = (HttpConnection) Connector.open(url);
      c.setRequestMethod(HttpConnection.POST);
      c.setRequestProperty("User-Agent",
                           "Profile/MIDP-1.0 Confirguration/CLDC-1.0");
      c.setRequestProperty("Accept_Language", "en-US");
      c.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
      if (params != null) {
        os = c.openOutputStream();
        os.write(params.getBytes());
      }

      is = c.openDataInputStream();
      while ( (ch = is.read()) != -1) {
        b.append( (char) ch);
        if (ch == '\n') {
          if (!b.toString().startsWith("<ok>")) {
            this.muestraError("No se han podido guardar todos datos. Error: " +
                              b.toString());
            error = true;
          }
        }
      }
    }
    catch (Exception e) {
      this.muestraError(e.toString());
      e.printStackTrace();
    }
    finally {
      //variables de la conexion
      if (is != null) {
        is.close();
      }
      if (c != null) {
        c.close();
      }
    }
    return error;
  }

  /**Procedimiento que realiza la lectura de datos desde la base de datos*/
  public void verServicios(String url) throws IOException {
        System.out.println("Url a la que navegamos: "+url);
    String grabandoError=new String("");
    Vector argumentos = new Vector();
    urls.removeAllElements();
    String servicio = null;
    boolean nodoHoja = false;
    parametrosEntrada.removeAllElements();
    Item parametroActual;
    HttpConnection c = null;
    InputStream is = null;
    StringBuffer b = new StringBuffer();
    int ch;
    pantallaDatos = new Form("");
    pantallaMenu = new List("", List.IMPLICIT);
    try {
      c = (HttpConnection) Connector.open(url);
      c.setRequestMethod(HttpConnection.GET);
      c.setRequestProperty("User-Agent",
                           "Profile/MIDP-1.0 Configuration/CLDC-1.0");
      c.setRequestProperty("Content-Language", "es-ES");

// leer----------------------------------------------------------------------
      is = c.openInputStream();
      while ( (ch = is.read()) != -1) {
        contador++;
        b.append( (char) ch);
        if (ch == '\n') {
          System.out.println(b.toString());
          if (b.toString().startsWith("<ser>")) { //servicio
            a�adeSubMenu(b);
          }
          else if (b.toString().startsWith("<url>")) {
            a�adeUrl(b);
          }
          else if (b.toString().startsWith("<amo>")) {
            a�adeAmo(b.toString());
          }
          else if (b.toString().startsWith("<mnu>")) { //inicio redireccionado (al partido en el que estemos)
            this.urlInicioPartido = extraer(b);
          }
          else if (b.toString().startsWith("<tic>")) { //ticker
            mostrarTicker(b);
          }
          else if (b.toString().startsWith("<mar>")) { //guardar marcador de goles en propia puerta
            servicio = extraer(b);
            local.setPP(Integer.parseInt(servicio.substring(0,
                servicio.indexOf(' '))));
            visitante.setPP(Integer.parseInt(servicio.substring(servicio.
                indexOf(' ') + 1, servicio.length())));
          }
          else if (b.toString().startsWith("<ent>")) { //argumentos de entrada de datos
            servicio = extraer(b);
            parametrosEntrada.addElement(servicio);
          }
          else if (b.toString().startsWith("<sal>")) { //datos de salida
            servicio = extraer(b);
          }
          else if (b.toString().startsWith("<res>")) { //resumen del partido
            try {
              this.resumenPartido();
            }
            catch (Exception e) {
              e.printStackTrace();
              muestraError("No se ha podido formar la pantalla de resumen. " +
                           e.toString());
            }
            return;
          }
          else if (b.toString().startsWith("<pla>")) { //tratamiento de una plantilla
            servicio = extraer(b);
            if (servicio.startsWith("amonestaciones local")) {
              mostrarPlantilla(1);
            }
            else if (servicio.startsWith("goles local")) {
              mostrarPlantilla(2);
            }
            else if (servicio.startsWith("convocatoria local")) {
              mostrarPlantilla(4);
            }

            else if (servicio.startsWith("amonestaciones vis")) {
              mostrarPlantilla(5);
            }
            else if (servicio.startsWith("goles vis")) {
              mostrarPlantilla(6);
            }
            else if (servicio.startsWith("convocatoria vis")) {
              mostrarPlantilla(8);
            }
            else {
              this.muestraError("Accion desconocida sobre una plantilla");
            }
            return;
          }
          else if (b.toString().startsWith("<pln>")) { //plantilla local nueva
            local = new equipo(extraer(b), "L");
          }
          else if (b.toString().startsWith("<pvn>")) { //plnatilla visitante nueva
            visitante = new equipo(extraer(b), "V");
          }
          else if (b.toString().startsWith("<plj>") ||
                   b.toString().startsWith("<pvj>")) { //jugador local
            //pasamos situacion, dni, nombre.
            servicio = extraer(b);
            int aux = servicio.indexOf(' ');
            String situacion = servicio.substring(0, aux);
            servicio = servicio.substring(aux + 1, servicio.length());
            aux = servicio.indexOf(' ');
            String goles = servicio.substring(0, aux);
            servicio = servicio.substring(aux + 1, servicio.length());
            aux = servicio.indexOf(' ');
            String amon = servicio.substring(0, aux);
            servicio = servicio.substring(aux + 1, servicio.length());
            aux = servicio.indexOf(' ');
            String dni = servicio.substring(0, aux);
            String nombre = servicio.substring(aux + 1, servicio.length());

            if (b.toString().startsWith("<plj>")) {
              local.setJugador(dni, nombre, situacion, goles, amon);
            }
            else {
              visitante.setJugador(dni, nombre, situacion, goles, amon);
            }
          }
          else if (b.toString().startsWith("<sus>")) {
            urlDeCambio = urlBase + extraer(b);
          }
          else if (b.toString().startsWith("<img>")) {
            servicio = extraer(b);
            ImageItem t = new ImageItem("", montarImagen(servicio),
                                        ImageItem.LAYOUT_CENTER, servicio);
            pantallaDatos.append(t);
          }
          else if (b.toString().startsWith("<tip>")) { //tipo del parametro
            parametroActual = nuevoTipoElemento(extraer(b), servicio,
                                                argumentos);
            pantallaDatos.append(parametroActual);
            parametroActual = null;
            argumentos.removeAllElements();
          }
          else if (b.toString().startsWith("<arg>")) {
            String aux = extraer(b);
            argumentos.addElement(aux);
          }
          else if (b.toString().startsWith("<err>")) {
            String aux = extraer(b);
            this.muestraError(aux);
            /*Si estamos validando y hay un error, entonces tendremos que hacer
             de nuevo una primera consulta.*/
            if (url.indexOf("validar") > 0) {
              primeraConsulta = true;
            }
            return;
          }
          else if (b.toString().startsWith("<mje>")) {
            String aux = extraer(b);
            this.muestraMensaje(aux);
            return;
          }
          //solo en el procedimiento de inicio se utiliza.
          else if (b.toString().startsWith("<sys>")) {
            fecha = extraer(b);
          }
          else if (b.toString().startsWith("<hoj>")) {
            nodoHoja = true;
          }
          else {
            System.out.println(
                "parametro mal pasado.................................");
                  grabandoError+=b.toString()+" ";
/*            this.muestraError(
                "La informacion que esta solicitando no se encuentra disponible" +
                b.toString());
            return;*/
          }
          b.delete(0, b.length());
        }
      }
      if (grabandoError.length()>10){
        this.muestraError(grabandoError+ "Url en la que sucede: "+url);
            return;
      }
      if (pantallaDatos.size() == 0) {
        pantallaMenu.addCommand(goCommand);
        pantallaMenu.addCommand(volverCommand);
        if (historial.size() == 1 || historial.size() == 0) {
          pantallaMenu.addCommand(exitCommand);
        }
        pantallaMenu.setCommandListener(this);
        display.setCurrent(pantallaMenu);
      }
      else {
        if (nodoHoja) {
          pantallaDatos.addCommand(inicioCommand);
        }
        else {
          pantallaDatos.addCommand(goCommand);
          pantallaDatos.addCommand(volverCommand);
        }
        pantallaDatos.setCommandListener(this);
        display.setCurrent(pantallaDatos);
      }
    }
    catch (Exception e) {
      this.muestraError("Error en el procedimiento principal. URL: " + url);
      e.printStackTrace();
    }
    finally {
      //variables de la conexion
      if (is != null) {
        is.close();
      }
      if (c != null) {
        c.close();
      }
    }
  }

  public void a�adeAmo(String cadena) {
    amoDesc.addElement(cadena);
  }

  protected void a�adirParametroEntrada(int tipo) {
    if ( (parametrosEntrada.size() % 2) != 0) {
      parametrosEntrada.addElement(Integer.toString(tipo));
    }
  }

  /**Procedimiento que crea un nuevo par�metro, ya sea de entrada o salida,
   * para a�adirle los datos incluidos en sucesivas <arg>.*/
  public Item nuevoTipoElemento(String tipo, String nombre, Vector argumentos) {
    if (tipo.startsWith("Choice")) {
      ChoiceGroup t = new ChoiceGroup(nombre, ChoiceGroup.EXCLUSIVE);
      for (int i = 0; i < argumentos.size(); i++) {
        t.append( (String) argumentos.elementAt(i), null);
      }
      a�adirParametroEntrada(0);
      return t;
    }
    else if (tipo.startsWith("String")) {
      String aux = new String(" ");
      for (int i = 0; i < argumentos.size(); i++) {
        aux += " " + (String) argumentos.elementAt(i);
      }
      StringItem t = new StringItem(nombre, aux);
      t.setText(aux);
      return t;
    }
    else if (tipo.startsWith("TextField")) {
      TextField t = new TextField(nombre, "", 20, TextField.ANY);
      String aux = null;
      for (int i = 0; i < argumentos.size(); i++) {
        aux += (String) argumentos.elementAt(i);
      }
      if (aux != null) {
        t.setString(aux);
      }
      a�adirParametroEntrada(1);
      return t;
    }
    else if (tipo.startsWith("TextNum")) {
      TextField t = new TextField(nombre, "0", 2, TextField.NUMERIC);
      if (argumentos.size() > 0) {
        t.setString( (String) argumentos.elementAt(0));

      }
      a�adirParametroEntrada(1);
      return t;
    }
    else if (tipo.startsWith("Date")) {
      DateField t = new DateField(nombre, DateField.DATE);
      a�adirParametroEntrada(2);
      return t;
    }
    else if (tipo.startsWith("Gauge")) {
      Gauge t = new Gauge(nombre, true, 10, 5);
      a�adirParametroEntrada(3);
      return t;
    }
    else {
      return null;
    }
  }

  /**procedimiento que guarda una URL en la bbdd de las urls*/
  protected void a�adeUrl(StringBuffer b) {
    String servicio = extraer(b);
    urls.addElement(servicio);
  }

  /**Procedimiento que dada una cadena de caracteres, elimina las etiquetas xml*/
  protected String extraer(StringBuffer b) {
    int h = b.toString().indexOf(">") + 1;
    int f = b.toString().indexOf("<", 1);
    return b.toString().substring(h, f);
  }

  /**Procedimiento que a�ade una entrada a una pantalla de menu*/
  private void a�adeSubMenu(StringBuffer b) {
    String servicio = extraer(b);
    pantallaMenu.append(servicio, null);
  }

  /**Procedimiento que establece el ticker a una pantalla a partir de una cadena dada*/
  private void mostrarTicker(StringBuffer b) {
    Ticker t = new Ticker(extraer(b));
    pantallaMenu.setTicker(t);
    pantallaDatos.setTicker(t);
  }

  protected void muestraError(Exception e) {
    Form pantallaError = new Form("");
    ImageItem img = new ImageItem("", this.montarImagen("er1"),
                                  ImageItem.LAYOUT_CENTER, "Error");
    StringItem cadena = new StringItem("Fall� la aplicacion: ", e.toString());
    StringItem aux = new StringItem(" ", " ");
    pantallaError.append(aux);
    pantallaError.append(img);
    pantallaError.append(cadena);
    pantallaError.addCommand(volverCommand);
    pantallaError.setCommandListener(this);
    display.setCurrent(pantallaError);
  }

  protected void muestraError(String s) {
    Form pantallaError = new Form("");
    ImageItem img = new ImageItem("", this.montarImagen("er2"),
                                  ImageItem.LAYOUT_CENTER, "Error");
    StringItem cadena = new StringItem("La ffrm te manda un mensaje: ", s);
    StringItem aux = new StringItem(" ", " ");
    pantallaError.append(aux);
    pantallaError.append(img);
    pantallaError.append(cadena);
    pantallaError.addCommand(volverCommand);
    pantallaError.setCommandListener(this);
    display.setCurrent(pantallaError);
  }

  protected void muestraMensaje(String s) {
    Form pantallaError = new Form("");
    ImageItem img = new ImageItem("", this.montarImagen("mje"),
                                  ImageItem.LAYOUT_CENTER, "Mensaje");
    StringItem cadena = new StringItem("La ffrm te manda un mensaje: ", s);
    StringItem aux = new StringItem(" ", " ");
    pantallaError.append(aux);
    pantallaError.append(img);
    pantallaError.append(cadena);
    pantallaError.addCommand(volverCommand);
    pantallaError.setCommandListener(this);
    display.setCurrent(pantallaError);
  }

  public static Image montarImagen(String url) {
    try {
      Image aux = Image.createImage("/" + url + ".png");
      //      Image img = Image.createImage(aux,20,20,100,100,4);
      return aux;
    }
    catch (Exception e) {
      System.out.println("No se ha cargado la imagen " + url + " " + e);
    }
    return null;
  }

  class Pantalla
      extends Canvas {
    String imagen;
    public Pantalla(String s) {
      imagen = new String(s);
    }

    public void paint(Graphics g) {
      Image img = null;
      g.setColor(99, 140, 49);
      g.fillRect(0, 0, getWidth(), getHeight());
      img = ffrms.montarImagen(imagen);
      g.drawImage(img, getWidth() / 2, getHeight() / 2,
                  Graphics.HCENTER | Graphics.VCENTER);
    }
  }

}
