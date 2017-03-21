package visualOld;

class JsonJavaClazz extends JsonJavaFile {
    private String superclass;

    JsonJavaClazz(String name, String fullname, String type, String superclass) {
        super(name, fullname, type);
        this.superclass = superclass;
    }
}
