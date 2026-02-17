package net.Gabou.identity2.util;
import java.util.ArrayList;
import java.lang.Object;
import java.lang.String;
import java.lang.Float;
import java.lang.Math;
import org.jetbrains.annotations.Nullable;
import net.Gabou.identity2.Identity2;
public class Calculator{


    public static Float calculate(Object input){
        if(input.getClass()==ArrayList.class){
            ArrayList<Object> inputb=(ArrayList<Object>)input;
            int type=(int)inputb.get(0);
            if(type==1){
                float finalValue=0;
                int numDone=0;
                for(int i=0;i<inputb.size();i++){
                    Object subvalue=inputb.get(i);
                    if(numDone!=0){
                        finalValue+=calculate(subvalue);
                    }
                    numDone+=1;
                    
                }
                return finalValue;
            }else if(type==2){
                float finalValue=0;
                int numDone=0;
                for(int i=0;i<inputb.size();i++){
                    Object subvalue=inputb.get(i);
                    if(numDone!=0){
                        if(numDone==1){
                            finalValue+=calculate(subvalue);
                        }else{
                            finalValue-=calculate(subvalue);
                        }
                    }
                    numDone+=1;
                    
                }
                return finalValue;
            }else if(type==3){
                float finalValue=0;
                int numDone=0;
                for(int i=0;i<inputb.size();i++){
                    Object subvalue=inputb.get(i);
                    if(numDone!=0){
                        if(numDone==1){
                            finalValue+=calculate(subvalue);
                        }else{
                            finalValue*=calculate(subvalue);
                        }
                    }
                    numDone+=1;
                    
                }
                return finalValue;
            }else if(type==4){
                float finalValue=0;
                int numDone=0;
                for(int i=0;i<inputb.size();i++){
                    Object subvalue=inputb.get(i);
                    if(numDone!=0){
                        if(numDone==1){
                            finalValue+=calculate(subvalue);
                        }else{
                            finalValue/=calculate(subvalue);
                        }
                    }
                    numDone+=1;
                    
                }
                return finalValue;
            }else if(type==5){
                float finalValue=0;
                int numDone=0;
                for(int i=0;i<inputb.size();i++){
                    Object subvalue=inputb.get(i);
                    if(numDone!=0){
                        if(numDone==1){
                            finalValue+=calculate(subvalue);
                        }else{
                            finalValue=(float)Math.pow(finalValue,calculate(subvalue));
                        }
                    }
                    numDone+=1;
                    
                }
                return finalValue;
            }else if(type==6){
                if(inputb.size()==3){
                    return UnaryFunctions.get(UnaryFunctionNames.indexOf((String)inputb.get(1))).getValue(
                        calculate(inputb.get(2))
                    );
                }else if(inputb.size()==4){
                    return BinaryFunctions.get(BinaryFunctionNames.indexOf((String)inputb.get(1))).getValue(
                        calculate(inputb.get(2)),
                        calculate(inputb.get(3))
                    );
                }
                return 0.0F;
            }else{
                return 0.0F;
            }
        }else if(input.getClass()==String.class){
            return calculate(parseString((String)input));
        }else{
            return (Float)input;
        }
    }
    
    public static Object parseString(String input){
        /*
        1. check if input has at least one of *+-/()
        */
        //Identity2.LOGGER.info(input);
        if(input.length()==0){
            return 0;
        }
        if(countChars(input,'(')!=countChars(input,')')){
            return 0;
        }
        String inputwbc=removeWithinBrackets(input,"(",")");
        ArrayList<Object> retval=new ArrayList(1);
        if(inputwbc.length()==0){
            return parseString(input.substring(1,input.length()-1));
        }
        
        if((countChars(inputwbc,'+',1)+countChars(inputwbc,'-',1)+countChars(inputwbc,'*',1)+countChars(inputwbc,'/',1))!=0){
            if((countChars(inputwbc,'+',1)+countChars(inputwbc,'-',1))==0){
                if(countChars(inputwbc,'*',1)!=0){
                    retval.add(3);
                    splitWhileRespectingBrackets(input,'*',"(",")").forEach(
                        splitAroundTimes->{
                            retval.ensureCapacity(retval.size()+1);
                            retval.add((Object)parseString(splitAroundTimes));
                        }
                    );
                    return retval;
                }
                else if(countChars(inputwbc,'/',1)!=0){
                    retval.add(4);
                    splitWhileRespectingBrackets(input,'/',"(",")").forEach(
                        splitAroundDivides->{
                            retval.ensureCapacity(retval.size()+1);
                            retval.add((Object)parseString(splitAroundDivides));
                        }
                    );
                    return retval;
                }else{
                    return 0;
                }
            }else{
                if(countChars(inputwbc,'+',1)!=0){
                    retval.add(1);
                    splitWhileRespectingBrackets(input,'+',"(",")").forEach(
                        splitAroundPlus->{
                            retval.ensureCapacity(retval.size()+1);
                            retval.add((Object)parseString(splitAroundPlus));
                        }
                    );
                    return retval;
                }
                else if(countChars(inputwbc,'-',1)!=0){
                    retval.add(2);
                    splitWhileRespectingBrackets(input,'-',"(",")").forEach(
                        splitAroundMinus->{
                            retval.ensureCapacity(retval.size()+1);
                            retval.add((Object)parseString(splitAroundMinus));
                        }
                    );
                    return retval;
                }else{
                    return 0;
                }
            }
        }else if(countChars(inputwbc,'^',1)!=0){
            retval.add(5);
            splitWhileRespectingBrackets(input,'^',"(",")").forEach(
                splitAroundTimes->{
                    retval.ensureCapacity(retval.size()+1);
                    retval.add((Object)parseString(splitAroundTimes));
                }
            );
            return retval;
        
        }else if((UnaryFunctionNames.contains(inputwbc))||(BinaryFunctionNames.contains(inputwbc))){
            int functionLen=inputwbc.length();
            retval.add(6);
            retval.ensureCapacity(2);
            retval.add(inputwbc);
            splitWhileRespectingBrackets(input.substring(1+functionLen,input.length()-1),',',"(",")").forEach(
                splitAroundComma->{
                    retval.ensureCapacity(retval.size()+1);
                    retval.add((Object)parseString(splitAroundComma));
                }
            );
            return retval;
        }else{
            return Float.parseFloat(input);
        }
    }
    public static int countChars(String input,char find,int max){
        int totalCount=0;
        for(int i=0;i<input.length();i++){
            if(input.charAt(i)==find){
                totalCount++;
                if(totalCount==max){
                    return totalCount;
                }
            }
        }
        return totalCount;
    }
    public static int countChars(String input,char find){
        int totalCount=0;
        for(int i=0;i<input.length();i++){
            if(input.charAt(i)==find){
                totalCount++;
            }
        }
        return totalCount;
    }
    public static ArrayList<String> splitWhileRespectingBrackets(String input,char splitAround, String leftBrackets, String rightBrackets){
        /*
        def splitWhileRespectingBrackets(inputValue,splitChar=' '):
            output=[""]
            depth=0
            for i in inputValue:
                if(i in '[<({'):
                    depth+=1
                if(i in ']>)}'):
                    depth-=1
                if(depth==0):
                    if(i==splitChar):
                        output.append('')
                    else:
                        output[-1]+=i
                else:
                    output[-1]+=i
            return output
        */
        
        ArrayList<String> output=new ArrayList(1);
        output.add("");
        int depth=0;
        for(int i=0;i<input.length();i++){
            if(countChars(leftBrackets,input.charAt(i),1)!=0){
                depth+=1;
            }else if(countChars(rightBrackets,input.charAt(i),1)!=0){
                depth-=1;
            }
            if(depth==0){
                if(input.charAt(i)==splitAround){
                    output.ensureCapacity(output.size()+1);
                    output.add("");
                }else{
                    output.set(output.size()-1,output.get(output.size()-1)+input.charAt(i));
                }
            }else{
                output.set(output.size()-1,output.get(output.size()-1)+input.charAt(i));
            }
        }
        return output;
    }
    public static ArrayList<String> splitWhileRespectingBrackets(String input,String splitAroundEntries, String leftBrackets, String rightBrackets){
        /*
        def splitWhileRespectingBrackets(inputValue,splitChar=' '):
            output=[""]
            depth=0
            for i in inputValue:
                if(i in '[<({'):
                    depth+=1
                if(i in ']>)}'):
                    depth-=1
                if(depth==0):
                    if(i==splitChar):
                        output.append('')
                    else:
                        output[-1]+=i
                else:
                    output[-1]+=i
            return output
        */
        
        ArrayList<String> output=new ArrayList(1);
        output.add("");
        int depth=0;
        for(int i=0;i<input.length();i++){
            if(countChars(leftBrackets,input.charAt(i),1)!=0){
                depth+=1;
            }else if(countChars(rightBrackets,input.charAt(i),1)!=0){
                depth-=1;
            }
            if(depth==0){
                if(countChars(splitAroundEntries,input.charAt(i),1)!=0){
                    output.ensureCapacity(output.size()+1);
                    output.add("");
                }else{
                    output.set(output.size()-1,output.get(output.size()-1)+input.charAt(i));
                }
            }else{
                output.set(output.size()-1,output.get(output.size()-1)+input.charAt(i));
            }
        }
        return output;
    }
    public static String removeWithinBrackets(String input,String leftBrackets, String rightBrackets){
        String output="";
        int depth=0;
        for(int i=0;i<input.length();i++){
            if(countChars(leftBrackets,input.charAt(i),1)!=0){
                depth+=1;
            }
            if(depth==0){
                output+=input.charAt(i);
            }
            if(countChars(rightBrackets,input.charAt(i),1)!=0){
                depth-=1;
            }
        }
        return output;
    }
    abstract static class UnaryCalculatorFunction{
        @Nullable
        public abstract Float getValue(Float arg);
    }
    abstract static class BinaryCalculatorFunction{
        @Nullable
        public abstract Float getValue(Float arga, Float argb);
    }
    static ArrayList<String> UnaryFunctionNames=new ArrayList(0);
    static ArrayList<UnaryCalculatorFunction> UnaryFunctions=new ArrayList(0);
    static ArrayList<String> BinaryFunctionNames=new ArrayList(0);
    static ArrayList<BinaryCalculatorFunction> BinaryFunctions=new ArrayList(0);
    public static void addUnaryFunction(String name,UnaryCalculatorFunction function){
        if(UnaryFunctionNames.contains(name)==false){
            UnaryFunctionNames.ensureCapacity(UnaryFunctionNames.size()+1);
            UnaryFunctionNames.add(name);
            UnaryFunctions.ensureCapacity(UnaryFunctions.size()+1);
            UnaryFunctions.add(function);
        }
    }
    public static void addBinaryFunction(String name,BinaryCalculatorFunction function){
        if(BinaryFunctionNames.contains(name)==false){
            BinaryFunctionNames.ensureCapacity(BinaryFunctionNames.size()+1);
            BinaryFunctionNames.add(name);
            BinaryFunctions.ensureCapacity(BinaryFunctions.size()+1);
            BinaryFunctions.add(function);
        }
    }
    static{
        
        addUnaryFunction("floor",new Calculator.UnaryCalculatorFunction() {
            @Override
            public Float getValue(Float arg){
                return (Float)(float)Math.floor((double)arg); 
            }
        });

        addBinaryFunction("min",new Calculator.BinaryCalculatorFunction() {
            @Override
            public Float getValue(Float arga,Float argb){
                return (Float)Math.min((float)arga,(float)argb); 
            }
        });
        addBinaryFunction("max",new Calculator.BinaryCalculatorFunction() {
            @Override
            public Float getValue(Float arga,Float argb){
                return (Float)Math.max((float)arga,(float)argb); 
            }
        });
    }
}