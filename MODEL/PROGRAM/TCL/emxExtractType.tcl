
tcl;

eval {
   if {[info host] == "sn732plp" } {
      source "c:/Program Files/TclPro1.3/win32-ix86/bin/prodebug.tcl"
   	  set cmd "debugger_eval"
   	  set xxx [debugger_init]
   } else {
   	  set cmd "eval"
   }
}
$cmd {

#  Set up array for symbolic name mapping
#
   set lsPropertyName [mql get env PROPERTYNAME]
   set lsPropertyTo [mql get env PROPERTYTO]
   set sTypeReplace "type "

   foreach sPropertyName $lsPropertyName sPropertyTo $lsPropertyTo {
      set sSchemaTest [lindex [split $sPropertyName "_"] 0]
      if {$sSchemaTest == "type"} {
         regsub $sTypeReplace $sPropertyTo "" sPropertyTo
         regsub "_" $sPropertyName "|" sSymbolicName
         set sSymbolicName [lindex [split $sSymbolicName |] 1]
         array set aSymbolic [list $sPropertyTo $sSymbolicName]
      }
   }

   set sFilter [mql get env 1]
   set bTemplate [mql get env 2]
   set bSpinnerAgentFilter [mql get env 3]
   set sGreaterThanEqualDate [mql get env 4]
   set sLessThanEqualDate [mql get env 5]

   set sAppend ""
   if {$sFilter != ""} {
      regsub -all "\134\052" $sFilter "ALL" sAppend
      regsub -all "\134\174" $sAppend "-" sAppend
      regsub -all "/" $sAppend "-" sAppend
      regsub -all ":" $sAppend "-" sAppend
      regsub -all "<" $sAppend "-" sAppend
      regsub -all ">" $sAppend "-" sAppend
      regsub -all " " $sAppend "" sAppend
      set sAppend "_$sAppend"
   }
   
   if {$sGreaterThanEqualDate != ""} {
      set sModDateMin [clock scan $sGreaterThanEqualDate]
   } else {
      set sModDateMin ""
   }
   if {$sLessThanEqualDate != ""} {
      set sModDateMax [clock scan $sLessThanEqualDate]
   } else {
      set sModDateMax ""
   }
   
   set sSpinnerPath [mql get env SPINNERPATH]
   if {$sSpinnerPath == ""} {
      set sOS [string tolower $tcl_platform(os)];
      set sSuffix [clock format [clock seconds] -format "%Y%m%d"]
      
      if { [string tolower [string range $sOS 0 5]] == "window" } {
         set sSpinnerPath "c:/temp/SpinnerAgent$sSuffix/Business";
      } else {
         set sSpinnerPath "/tmp/SpinnerAgent$sSuffix/Business";
      }
      file mkdir $sSpinnerPath
   }

   set sPath "$sSpinnerPath/Business/SpinnerTypeData$sAppend.xls"
   set lsType [split [mql list type $sFilter] \n]
   set sFile "Name\tRegistry Name\tParent Type\tAbstract (boolean)\tDescription\tAttributes (use \"|\" delim)\tMethods (use \"|\" delim)\tHidden (boolean)\tSparse (boolean)\tIcon File\n"
   set sMxVersion [mql get env MXVERSION]
   if {$sMxVersion == ""} {
      set sMxVersion "2012"
   }
   
   if {!$bTemplate} {
      foreach sType $lsType {
         set bPass TRUE
         if {$sMxVersion > 8.9} {
            set sModDate [mql print type $sType select modified dump]
            set sModDate [clock scan [clock format [clock scan $sModDate] -format "%m/%d/%Y"]]
            if {$sModDateMin != "" && $sModDate < $sModDateMin} {
               set bPass FALSE
            } elseif {$sModDateMax != "" && $sModDate > $sModDateMax} {
               set bPass FALSE
            }
         }
         
         if {($bPass == "TRUE") && ($bSpinnerAgentFilter != "TRUE" || [mql print type $sType select property\[SpinnerAgent\] dump] != "")} {
            set sName [mql print type $sType select name dump]
            set sOrigName ""
            catch {set sOrigName $aSymbolic($sType)} sMsg
            regsub -all " " $sType "" sOrigNameTest
            if {$sOrigNameTest == $sOrigName} {
               set sOrigName $sType
            }
            set sDescription [mql print type $sType select description dump]
            set sHidden [mql print type $sType select hidden dump]
            set bSparse [mql print type $sType select sparse dump]
            set slsAttribute [mql print type $sType select immediateattribute dump " | "]
            set sDerived [mql print type $sType select derived dump]
   
            if {$sDerived != ""} {
               set lsMethod [split [mql print type $sType select method dump |] |]
               set lsMethodDerived [split [mql print type $sDerived select method dump |] |]
               set lsMethodDerivative ""
               foreach sMethod $lsMethod {
                  if {[lsearch $lsMethodDerived $sMethod] < 0} {
                     lappend lsMethodDerivative $sMethod
                  }
               }
               set slsMethod ""
               if {[llength $lsMethodDerivative] > 1} {
                  set slsMethod [join $lsMethodDerivative " | "]
               }
         	    if {[llength $lsMethodDerivative] == 1} {
   	             set slsMethod [lindex $lsMethodDerivative 0]
   	          }
   
            } else {
               set slsMethod [mql print type $sType select method dump " | "]
            }
   
            set lsTypeData [split [mql print type $sType] \n]
            set bAbstract false
            foreach sTypeData $lsTypeData {
               set sTypeData [string trim $sTypeData]
               if {[string range $sTypeData 0 7] == "abstract"} {
                  regsub "abstract " $sTypeData "" bAbstract
                  break
               }
            }
            append sFile "$sName\t$sOrigName\t$sDerived\t$bAbstract\t$sDescription\t$slsAttribute\t$slsMethod\t$sHidden\t$bSparse\n"
         }
      }
   }
   set iFile [open $sPath w]
   puts $iFile $sFile
   close $iFile
   puts "Type data loaded in file $sPath"
}
