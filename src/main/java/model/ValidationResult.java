package model;

import java.util.ArrayList;
import java.util.List;

public class ValidationResult {
    private final List<String> errors = new ArrayList<>();
    public void add(String e){ if(e!=null && !e.isEmpty()) errors.add(e); }
    public List<String> getErrors(){ return errors; }
    public boolean isOk(){ return errors.isEmpty(); }
}
