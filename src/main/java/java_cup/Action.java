package java_cup;

public interface Action {

    Action ERROR_ACTION = new Action() {

        public int type() {
            return ERROR;
        }
    };
    
    Action NONASSOC_ACTION = new Action() {

        public int type() {
            return NONASSOC;
        }
    };

    int ERROR = 0;
    int SHIFT = 1;
    int REDUCE = 2;
    int NONASSOC = 3;

    int type();
}
