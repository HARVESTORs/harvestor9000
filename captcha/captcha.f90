! fortran captcha. dont ask
program captcha
  integer :: x
  read *, x
  if (x == 42) then
    print *, 'human'
  else
    print *, 'robot (probably)'
  end if
end program
